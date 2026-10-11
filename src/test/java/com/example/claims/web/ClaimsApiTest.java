package com.example.claims.web;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.example.claims.support.InMemoryClaimStore;
import com.example.claims.support.Lines;

/**
 * Exercises the HTTP API end to end through Spring MVC. Claims are kept in memory so the
 * test needs no running MongoDB; {@code docker compose up} covers the real database.
 */
@SpringBootTest(properties = "claims.import.sample-file=src/test/resources/legacy_claims_export.txt")
@AutoConfigureMockMvc
class ClaimsApiTest {

    @TestConfiguration
    static class InMemoryStoreConfig {
        @Bean
        @Primary
        InMemoryClaimStore inMemoryClaimStore() {
            return new InMemoryClaimStore();
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private InMemoryClaimStore store;

    @BeforeEach
    void clearStore() {
        store.clear();
    }

    @Test
    void importsTheSampleFileAndReportsRejections() throws Exception {
        mvc.perform(post("/imports/sample"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordsRead", is(39)))
                .andExpect(jsonPath("$.recordsAccepted", is(30)))
                .andExpect(jsonPath("$.recordsRejected", is(9)))
                .andExpect(jsonPath("$.rejections[0].lineNumber", is(6)))
                .andExpect(jsonPath("$.rejections[0].claimId", is("CLM0000005")))
                .andExpect(jsonPath("$.trailerCountMatches", is(false)));
    }

    @Test
    void importsAnUploadedFile() throws Exception {
        String content = String.join("\n",
                Lines.header("20261001"), Lines.validDetail("CLM0000001"), Lines.trailer(1));
        MockMultipartFile file = new MockMultipartFile("file", "export.txt", "text/plain",
                content.getBytes(StandardCharsets.ISO_8859_1));

        mvc.perform(multipart("/imports").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordsAccepted", is(1)));
    }

    @Test
    void returnsAClaimById() throws Exception {
        mvc.perform(post("/imports/sample")).andExpect(status().isOk());

        mvc.perform(get("/claims/CLM0000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName", is("BENNETT")))
                .andExpect(jsonPath("$.status", is("OP")))
                .andExpect(jsonPath("$.statusDescription", is("Open")))
                .andExpect(jsonPath("$.weeklyBenefitAmount", is(334.00)));
    }

    @Test
    void returns404ForAnUnknownClaim() throws Exception {
        mvc.perform(get("/claims/CLM9999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", is("No claim with ID CLM9999999.")));
    }

    @Test
    void filtersClaimsByStatus() throws Exception {
        mvc.perform(post("/imports/sample")).andExpect(status().isOk());

        mvc.perform(get("/claims").param("status", "DN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", everyItem(is("DN"))));
    }

    @Test
    void returns400ForAnUnknownStatusCode() throws Exception {
        mvc.perform(get("/claims").param("status", "XX"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("Status 'XX' is not a valid code. Use one of: OP, AP, DN, CL.")));
    }

    @Test
    void secondImportCreatesNoDuplicates() throws Exception {
        mvc.perform(post("/imports/sample")).andExpect(status().isOk());
        mvc.perform(post("/imports/sample")).andExpect(status().isOk());

        mvc.perform(get("/claims")).andExpect(jsonPath("$", hasSize(30)));
    }
}
