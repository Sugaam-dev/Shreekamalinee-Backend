package com.pmrgsolution.features.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrgsolution.core.security.JwtAuthenticationFilter;
import com.pmrgsolution.core.security.JwtUtils;
import com.pmrgsolution.core.security.LogoutService;
import com.pmrgsolution.features.order.dto.OrderResponse;
import com.pmrgsolution.features.order.service.OrderService;
import com.pmrgsolution.features.payment.service.PaymentService;
import com.pmrgsolution.features.settings.dto.StoreSettingsResponse;
import com.pmrgsolution.features.settings.service.StoreSettingsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OrderController Integration / Slice Tests")
class OrderControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private OrderService orderService;
    @MockBean private PaymentService paymentService;
    @MockBean private StoreSettingsService storeSettingsService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private LogoutService logoutService;

    @Test
    @DisplayName("GET /api/v1/orders/bank-details -> Returns only restricted BankDetailsResponse")
    void getBankDetails_returnsRestrictedFields() throws Exception {
        StoreSettingsResponse settings = new StoreSettingsResponse();
        settings.setAccountHolderName("Shreekamalinee");
        settings.setAccountNumber("1234567890");
        settings.setIfscCode("HDFC0001234");
        settings.setBankName("HDFC Bank");
        settings.setBranchName("Main Branch");
        settings.setUpiId("shreekamalinee@hdfcbank");
        settings.setQrCodeUrl("https://example.com/qr.png");

        when(storeSettingsService.getStoreSettings()).thenReturn(settings);

        mockMvc.perform(get("/api/v1/orders/bank-details")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountHolderName").value("Shreekamalinee"))
                .andExpect(jsonPath("$.upiId").value("shreekamalinee@hdfcbank"))
                .andExpect(jsonPath("$.standardShippingFee").doesNotExist())
                .andExpect(jsonPath("$.codHandlingFee").doesNotExist());
    }
}
