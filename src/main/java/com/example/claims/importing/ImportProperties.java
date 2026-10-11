package com.example.claims.importing;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param sampleFile path of the export loaded by {@code POST /imports/sample}
 */
@ConfigurationProperties(prefix = "claims.import")
public record ImportProperties(String sampleFile) {
}
