package com.adorsys.webank;

import com.adorsys.webank.dto.KycInfoRequest;
import com.adorsys.webank.dto.response.KycInfoResponse;
import com.adorsys.webank.service.KycServiceApi;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.repository.UserDocumentsRepository;
import com.adorsys.webank.repository.OtpRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.fail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(NoMethodSecurityConfig.class)
@TestPropertySource(properties = {"jwt.expiration-time-ms=3600000", "management.health.mail.enabled=false", "spring.mail.username=test@example.com"})
class KycRestServerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KycServiceApi kycServiceApi;
    @MockBean
    private PersonalInfoRepository personalInfoRepository;
    @MockBean
    private UserDocumentsRepository userDocumentsRepository;
    @MockBean
    private JavaMailSender javaMailSender;
    @MockBean
    private OtpRequestRepository otpRequestRepository;

    @Test
    @WithMockUser(username = "testuser", roles = {"ACCOUNT_CERTIFIED"})
    void sendKycinfo_ReturnsOk() {
        KycInfoResponse response = new KycInfoResponse();
        response.setMessage("KYC Info submitted successfully");
        when(kycServiceApi.sendKycInfo(any(), any())).thenReturn(response);

        KycInfoRequest request = new KycInfoRequest();
        request.setAccountId("acc123");
        request.setIdNumber("id123");
        request.setExpiryDate("2025-12-31");

        ObjectMapper objectMapper = new ObjectMapper();
        String json;
        try {
            json = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            fail("JSON serialization failed", e);
            return;
        }

        try {
            mockMvc.perform(post("/api/prs/kyc/info")
                    .header("Authorization", "Bearer testtoken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("KYC Info submitted successfully"));
        } catch (Exception e) {
            fail("Test failed due to unexpected exception", e);
        }
    }

    // Negative case: missing required field (idNumber)
    @Test
    @WithMockUser(username = "testuser", roles = {"ACCOUNT_CERTIFIED"})
    void sendKycinfo_MissingIdNumber_ReturnsBadRequest() {
        KycInfoRequest request = new KycInfoRequest();
        request.setAccountId("acc123");
        request.setExpiryDate("2025-12-31");
        // idNumber is missing

        ObjectMapper objectMapper = new ObjectMapper();
        String json;
        try {
            json = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            fail("JSON serialization failed", e);
            return;
        }

        try {
            mockMvc.perform(post("/api/prs/kyc/info")
                    .header("Authorization", "Bearer testtoken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail("Test failed due to unexpected exception", e);
        }
    }

    // Negative case: invalid expiryDate format
    @Test
    @WithMockUser(username = "testuser", roles = {"ACCOUNT_CERTIFIED"})
    void sendKycinfo_InvalidExpiryDateFormat_ReturnsBadRequest() {
        KycInfoRequest request = new KycInfoRequest();
        request.setAccountId("acc123");
        request.setIdNumber("id12345");
        request.setExpiryDate("31-12-2025"); // Invalid format

        ObjectMapper objectMapper = new ObjectMapper();
        String json;
        try {
            json = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            fail("JSON serialization failed", e);
            return;
        }

        try {
            mockMvc.perform(post("/api/prs/kyc/info")
                    .header("Authorization", "Bearer testtoken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail("Test failed due to unexpected exception", e);
        }
    }

    // Negative case: service throws exception (simulate internal server error)
    @Test
    @WithMockUser(username = "testuser", roles = {"ACCOUNT_CERTIFIED"})
    void sendKycinfo_ServiceThrowsException_ReturnsServerError() {
        when(kycServiceApi.sendKycInfo(any(), any())).thenThrow(new RuntimeException("Simulated service error"));

        KycInfoRequest request = new KycInfoRequest();
        request.setAccountId("acc123");
        request.setIdNumber("id12345");
        request.setExpiryDate("2025-12-31");

        ObjectMapper objectMapper = new ObjectMapper();
        String json;
        try {
            json = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            fail("JSON serialization failed", e);
            return;
        }

        try {
            mockMvc.perform(post("/api/prs/kyc/info")
                    .header("Authorization", "Bearer testtoken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isInternalServerError());
        } catch (Exception e) {
            fail("Test failed due to unexpected exception", e);
        }
    }
} 