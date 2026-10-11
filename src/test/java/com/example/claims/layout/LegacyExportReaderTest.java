package com.example.claims.layout;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.claims.support.Lines;

class LegacyExportReaderTest {

    private final LegacyExportReader reader = new LegacyExportReader();

    @Test
    void readsHeaderDetailsAndTrailer() {
        LegacyExport export = reader.read(List.of(
                Lines.header("20261001"),
                Lines.validDetail("CLM0000001"),
                Lines.validDetail("CLM0000002"),
                Lines.trailer(2)));

        assertThat(export.exportDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(export.sourceSystem()).isEqualTo("STATE-CLAIMS-LEGACY");
        assertThat(export.trailerCount()).isEqualTo(2);
        assertThat(export.details()).extracting(DetailLine::lineNumber).containsExactly(2, 3);
        assertThat(export.structureNotes()).isEmpty();
    }

    @Test
    void lineNumbersCountTheHeader() {
        LegacyExport export = reader.read(List.of(Lines.header("20261001"), Lines.validDetail("CLM0000001")));
        assertThat(export.details().get(0).lineNumber()).isEqualTo(2);
    }

    @Test
    void keepsTrailingSpacesButDropsWindowsLineEndings() {
        String header = Lines.header("20261001");
        LegacyExport export = reader.read(List.of(header + "\r", Lines.validDetail("CLM0000001") + "\r"));
        assertThat(export.details().get(0).text()).hasSize(80);
    }

    @Test
    void notesMissingHeaderTrailerAndUnknownRecordTypes() {
        LegacyExport export = reader.read(List.of(Lines.validDetail("CLM0000001"), "X junk"));

        assertThat(export.structureNotes()).anyMatch(n -> n.contains("no header"))
                .anyMatch(n -> n.contains("no trailer"))
                .anyMatch(n -> n.contains("Line 2") && n.contains("unknown record type"));
        assertThat(export.trailerCount()).isNull();
    }

    @Test
    void fieldReturnsNullWhenLineIsTooShort() {
        assertThat(RecordLayout.STATUS.rawFrom("DCLM0000001")).isNull();
        assertThat(RecordLayout.CLAIM_ID.rawFrom("DCLM0000001")).isEqualTo("CLM0000001");
    }
}
