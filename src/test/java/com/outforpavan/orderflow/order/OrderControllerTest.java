package com.outforpavan.orderflow.order;

import com.outforpavan.orderflow.api.InsufficientStockException;
import com.outforpavan.orderflow.api.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.Instant;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean OrderService orders;

    @Test
    void createsAnOrderWithALocationAndServerCalculatedPrice() throws Exception {
        given(orders.create(any())).willReturn(new OrderResponse(10L, 2L, 3,
                new BigDecimal("12.50"), new BigDecimal("37.50"), Instant.parse("2026-09-25T10:00:00Z")));
        mvc.perform(post("/api/orders").contentType("application/json")
                        .content("{\"productId\":2,\"quantity\":3}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/orders/10"))
                .andExpect(jsonPath("$.total").value(37.50));
    }

    @Test
    void rejectsMissingProductAndNonPositiveQuantityBeforeTheService() throws Exception {
        mvc.perform(post("/api/orders").contentType("application/json")
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
        verifyNoInteractions(orders);
    }

    @Test
    void rejectsFractionalQuantityInsteadOfSilentlyTruncatingIt() throws Exception {
        mvc.perform(post("/api/orders").contentType("application/json")
                        .content("{\"productId\":2,\"quantity\":1.5}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(orders);
    }

    @Test
    void reportsStockConflictAs409() throws Exception {
        given(orders.create(any())).willThrow(new InsufficientStockException(2L, 3, 1));
        mvc.perform(post("/api/orders").contentType("application/json")
                        .content("{\"productId\":2,\"quantity\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void reportsAMissingOrderAs404() throws Exception {
        given(orders.get(999L)).willThrow(new ResourceNotFoundException("Order 999 not found"));
        mvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Order 999 not found"));
    }
}
