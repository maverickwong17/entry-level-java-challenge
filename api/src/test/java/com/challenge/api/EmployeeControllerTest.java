package com.challenge.api.controller;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeControllerTest {

    private static final String BASE_URL = "/api/v1/employee";

    private static final String VALID_BODY =
            """
            {
              "firstName": "Katherine",
              "lastName": "Johnson",
              "salary": 95000,
              "age": 38,
              "jobTitle": "Mathematician",
              "email": "katherine.johnson@example.com",
              "contractHireDate": "2024-05-01T00:00:00Z"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAllReturnsSeededEmployees() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(0)))
                .andExpect(jsonPath("$[0].uuid", notNullValue()))
                .andExpect(jsonPath("$[0].fullName", notNullValue()));
    }

    @Test
    void createReturns201WithLocationAndEmployee() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.uuid", notNullValue()))
                .andExpect(jsonPath("$.fullName").value("Katherine Johnson"))
                .andExpect(jsonPath("$.contractTerminationDate").doesNotExist());
    }

    @Test
    void createdEmployeeCanBeFetchedByUuid() throws Exception {
        String location = mockMvc.perform(
                        post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(get(URI.create(location)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("katherine.johnson@example.com"));
    }

    @Test
    void createRejectsEmptyObjectListingEveryMissingField() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(7)));
    }

    @Test
    void createRejectsTerminationBeforeHire() throws Exception {
        String body = VALID_BODY.replace(
                "\"contractHireDate\": \"2024-05-01T00:00:00Z\"",
                "\"contractHireDate\": \"2024-05-01T00:00:00Z\", \"contractTerminationDate\": \"2024-01-01T00:00:00Z\"");

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsMalformedJson() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsUnparseableDate() throws Exception {
        String body = VALID_BODY.replace("2024-05-01T00:00:00Z", "01/05/2024");

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsMissingBody() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }

    @Test
    void getReturns404ForUnknownUuid() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void getReturns400ForMalformedUuid() throws Exception {
        mockMvc.perform(get(BASE_URL + "/not-a-uuid")).andExpect(status().isBadRequest());
    }
}
