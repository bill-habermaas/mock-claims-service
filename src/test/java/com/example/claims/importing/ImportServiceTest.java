package com.example.claims.importing;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.claims.importing.ImportSummary.Issue;
import com.example.claims.layout.LegacyExportReader;
import com.example.claims.support.InMemoryClaimStore;
import com.example.claims.support.Lines;
import com.example.claims.validation.ClaimMapper;
import com.example.claims.validation.ClaimValidator;

class ImportServiceTest {

    private InMemoryClaimStore store;
    private ImportService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryClaimStore();
        service = new ImportService(new LegacyExportReader(), new ClaimValidator(), new ClaimMapper(), store);
    }

    private static List<String> sampleFile() throws IOException, URISyntaxException {
        Path path = Path.of(Objects.requireNonNull(
                ImportServiceTest.class.getResource("/legacy_claims_export.txt")).toURI());
        return Files.readAllLines(path, StandardCharsets.ISO_8859_1);
    }

    @Test
    void sampleFileProducesTheExpectedCounts() throws Exception {
        ImportSummary summary = service.importLines("sample", sampleFile());

        assertThat(summary.recordsRead()).isEqualTo(39);
        assertThat(summary.recordsRejected()).isEqualTo(9);
        assertThat(summary.recordsAccepted()).isEqualTo(30);
        assertThat(summary.recordsAccepted() + summary.recordsRejected()).isEqualTo(summary.recordsRead());
        assertThat(store.size()).isEqualTo(30);
    }

    @Test
    void everySeededDefectIsCaughtOrHandledByADecision() throws Exception {
        ImportSummary summary = service.importLines("sample", sampleFile());

        assertThat(summary.rejections()).extracting(Issue::lineNumber)
                .containsExactly(6, 10, 18, 22, 26, 31, 34, 35, 38);
        assertThat(reasonFor(summary, 6)).contains("20250231");
        assertThat(reasonFor(summary, 10)).contains("'XX'");
        assertThat(reasonFor(summary, 18)).contains("Weekly benefit amount");
        assertThat(reasonFor(summary, 22)).contains("Last name is blank");
        assertThat(reasonFor(summary, 26)).contains("earlier than date of birth");
        assertThat(reasonFor(summary, 38)).contains("52 characters");
        assertThat(summary.rejections()).allSatisfy(issue -> assertThat(issue.claimId()).isNotBlank());
    }

    @Test
    void decision1_unknownDateOfBirthLoadsWithAWarning() throws Exception {
        ImportSummary summary = service.importLines("sample", sampleFile());

        assertThat(store.findById("CLM0000013")).hasValueSatisfying(c -> {
            assertThat(c.dateOfBirth()).isNull();
            assertThat(c.dateOfBirthUnknown()).isTrue();
        });
        assertThat(summary.warnings()).anyMatch(w -> Integer.valueOf(14).equals(w.lineNumber()));
    }

    @Test
    void decision2_exactDuplicateLineIsSkippedAndTheFirstCopyLoads() throws Exception {
        ImportSummary summary = service.importLines("sample", sampleFile());

        assertThat(reasonFor(summary, 31)).isEqualTo("Exact duplicate of line 30; this copy was skipped.");
        assertThat(store.findById("CLM0000029")).hasValueSatisfying(c -> assertThat(c.sourceLineNumber()).isEqualTo(30));
    }

    @Test
    void decision3_conflictingVersionsOfAClaimAreBothHeldBack() throws Exception {
        ImportSummary summary = service.importLines("sample", sampleFile());

        assertThat(reasonFor(summary, 34)).contains("line 35").contains("held for agency review");
        assertThat(reasonFor(summary, 35)).contains("line 34");
        assertThat(store.findById("CLM0000032")).isEmpty();
    }

    @Test
    void decision4_trailerMismatchIsReportedButRecordsStillLoad() throws Exception {
        ImportSummary summary = service.importLines("sample", sampleFile());

        assertThat(summary.trailerCount()).isEqualTo(41);
        assertThat(summary.trailerCountMatches()).isFalse();
        assertThat(summary.warnings()).anyMatch(w -> w.reason().contains("Trailer states 41")
                && w.reason().contains("contains 39"));
        assertThat(summary.recordsAccepted()).isPositive();
    }

    @Test
    void importingTheSameFileTwiceCreatesNoDuplicates() throws Exception {
        service.importLines("sample", sampleFile());
        ImportSummary second = service.importLines("sample", sampleFile());

        assertThat(second.recordsAccepted()).isEqualTo(30);
        assertThat(store.size()).isEqualTo(30);
    }

    @Test
    void aBadRecordDoesNotStopTheImport() {
        ImportSummary summary = service.importLines("small", List.of(
                Lines.header("20261001"),
                "Dgarbage",
                Lines.validDetail("CLM0000001"),
                Lines.trailer(2)));

        assertThat(summary.recordsRejected()).isEqualTo(1);
        assertThat(summary.recordsAccepted()).isEqualTo(1);
        assertThat(summary.trailerCountMatches()).isTrue();
        assertThat(summary.warnings()).isEmpty();
    }

    private static String reasonFor(ImportSummary summary, int lineNumber) {
        return summary.rejections().stream()
                .filter(i -> i.lineNumber() == lineNumber)
                .map(Issue::reason)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No rejection for line " + lineNumber));
    }
}
