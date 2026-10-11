package com.example.claims.importing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.claims.claim.Claim;
import com.example.claims.claim.ClaimStore;
import com.example.claims.importing.ImportSummary.Issue;
import com.example.claims.layout.DetailLine;
import com.example.claims.layout.LegacyExport;
import com.example.claims.layout.LegacyExportReader;
import com.example.claims.layout.RecordLayout;
import com.example.claims.validation.ClaimMapper;
import com.example.claims.validation.ClaimValidator;

/**
 * Loads a legacy export: keeps every valid record, reports every invalid one.
 *
 * <p>A bad record never stops the import. Claims are stored by claim ID, so importing
 * the same file twice leaves one copy of each claim.
 */
@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final LegacyExportReader reader;
    private final ClaimValidator validator;
    private final ClaimMapper mapper;
    private final ClaimStore store;

    public ImportService(LegacyExportReader reader, ClaimValidator validator, ClaimMapper mapper, ClaimStore store) {
        this.reader = reader;
        this.validator = validator;
        this.mapper = mapper;
        this.store = store;
    }

    public ImportSummary importLines(String source, List<String> lines) {
        LegacyExport export = reader.read(lines);
        List<Issue> rejections = new ArrayList<>();
        List<Issue> warnings = new ArrayList<>();
        export.structureNotes().forEach(note -> warnings.add(new Issue(null, null, note)));

        // Pass 1: drop exact duplicate lines and invalid lines; group the rest by claim ID.
        Map<String, Integer> firstLineWithText = new HashMap<>();
        Map<String, List<DetailLine>> validByClaimId = new LinkedHashMap<>();
        for (DetailLine line : export.details()) {
            Integer earlier = firstLineWithText.putIfAbsent(line.text(), line.lineNumber());
            if (earlier != null) {
                // Decision 2: an identical line carries no new information, so it is skipped.
                rejections.add(new Issue(line.lineNumber(), line.claimIdForReport(),
                        "Exact duplicate of line " + earlier + "; this copy was skipped."));
                continue;
            }
            List<String> reasons = validator.validate(line);
            if (!reasons.isEmpty()) {
                rejections.add(new Issue(line.lineNumber(), line.claimIdForReport(), String.join(" ", reasons)));
                continue;
            }
            validByClaimId.computeIfAbsent(line.trimmed(RecordLayout.CLAIM_ID), id -> new ArrayList<>()).add(line);
        }

        // Pass 2: resolve claim IDs that appear with different values, then build claims.
        List<Claim> accepted = new ArrayList<>();
        for (Map.Entry<String, List<DetailLine>> entry : validByClaimId.entrySet()) {
            List<DetailLine> versions = entry.getValue();
            if (versions.size() > 1) {
                // Decision 3: the file does not say which version is current, so neither is loaded.
                rejectConflictingVersions(entry.getKey(), versions, rejections);
                continue;
            }
            Claim claim = mapper.toClaim(versions.get(0));
            if (claim.dateOfBirthUnknown()) {
                // Decision 1: loaded, but flagged so staff can fill in the date.
                warnings.add(new Issue(claim.sourceLineNumber(), claim.claimId(),
                        "Date of birth recorded as unknown (00000000). Claim loaded with no date of birth; "
                                + "the filed-after-birth check was skipped."));
            }
            accepted.add(claim);
        }
        store.saveAll(accepted);

        // Decision 4: a trailer mismatch is reported, but the records actually in the file still load.
        int read = export.details().size();
        boolean trailerMatches = Objects.equals(export.trailerCount(), read);
        if (!trailerMatches && export.trailerCount() != null) {
            warnings.add(new Issue(null, null, "Trailer states " + export.trailerCount()
                    + " detail records; the file contains " + read
                    + ". All records present were processed. The file may be incomplete; confirm with the source system."));
        }

        rejections.sort(Comparator.comparing(Issue::lineNumber));
        ImportSummary summary = new ImportSummary(source, export.exportDate(), export.sourceSystem(),
                read, accepted.size(), rejections.size(), export.trailerCount(), trailerMatches,
                List.copyOf(rejections), List.copyOf(warnings));
        log.info("Imported {}: read={} accepted={} rejected={} warnings={}",
                source, read, summary.recordsAccepted(), summary.recordsRejected(), warnings.size());
        return summary;
    }

    private static void rejectConflictingVersions(String claimId, List<DetailLine> versions, List<Issue> rejections) {
        for (DetailLine line : versions) {
            String others = versions.stream()
                    .filter(other -> other != line)
                    .map(other -> String.valueOf(other.lineNumber()))
                    .collect(Collectors.joining(", "));
            rejections.add(new Issue(line.lineNumber(), claimId,
                    "Claim ID also appears on line " + others + " with different values. "
                            + "Neither version was loaded; held for agency review."));
        }
    }
}
