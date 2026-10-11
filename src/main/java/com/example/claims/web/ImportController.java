package com.example.claims.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.claims.importing.ImportProperties;
import com.example.claims.importing.ImportService;
import com.example.claims.importing.ImportSummary;

@RestController
@RequestMapping("/imports")
public class ImportController {

    // The mainframe export is single-byte text; ISO-8859-1 never fails to decode a byte.
    private static final java.nio.charset.Charset EXPORT_CHARSET = StandardCharsets.ISO_8859_1;

    private final ImportService importService;
    private final ImportProperties properties;

    public ImportController(ImportService importService, ImportProperties properties) {
        this.importService = importService;
        this.properties = properties;
    }

    /** Imports an uploaded export file (multipart form field {@code file}). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportSummary importUpload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is empty.");
        }
        List<String> lines = new String(file.getBytes(), EXPORT_CHARSET).lines().toList();
        return importService.importLines(file.getOriginalFilename(), lines);
    }

    /** Imports the export file configured as {@code claims.import.sample-file}. */
    @PostMapping("/sample")
    public ImportSummary importSample() throws IOException {
        if (properties.sampleFile() == null || properties.sampleFile().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No sample file is configured.");
        }
        Path path = Path.of(properties.sampleFile());
        if (!Files.isReadable(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sample file not found: " + path);
        }
        return importService.importLines(path.getFileName().toString(), Files.readAllLines(path, EXPORT_CHARSET));
    }
}
