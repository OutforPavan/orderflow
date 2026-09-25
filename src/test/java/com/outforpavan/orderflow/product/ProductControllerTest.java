package com.outforpavan.orderflow.product;

import com.outforpavan.orderflow.api.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void createsAProductWithALocationHeader() throws Exception {
        when(productService.create(any(CreateProductRequest.class)))
                .thenReturn(new ProductResponse(42L, "Keyboard", new BigDecimal("99.90"), 5));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Keyboard","price":99.90,"stock":5}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/products/42"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andExpect(jsonPath("$.price").value(99.90))
                .andExpect(jsonPath("$.stock").value(5));
        verify(productService).create(new CreateProductRequest("Keyboard", new BigDecimal("99.90"), 5));
    }

    @Test
    void readsAProduct() throws Exception {
        when(productService.get(42L))
                .thenReturn(new ProductResponse(42L, "Keyboard", new BigDecimal("99.90"), 5));

        mockMvc.perform(get("/api/products/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.stock").value(5));
    }

    @Test
    void changesAProductPrice() throws Exception {
        when(productService.changePrice(42L, new UpdateProductPriceRequest(new BigDecimal("89.50"))))
                .thenReturn(new ProductResponse(42L, "Keyboard", new BigDecimal("89.50"), 5));

        mockMvc.perform(patch("/api/products/42/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":89.50}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(89.50));
    }

    @ParameterizedTest
    @MethodSource("invalidProductRequests")
    void rejectsInvalidProductFieldsBeforeCallingTheService(String request, String field) throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/products"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem(field)));
        verifyNoInteractions(productService);
    }

    static Stream<Arguments> invalidProductRequests() {
        return Stream.of(
                Arguments.of("{}", "name"),
                Arguments.of("{\"name\":\"   \",\"price\":10,\"stock\":1}", "name"),
                Arguments.of("{\"name\":\"" + "x".repeat(121) + "\",\"price\":10,\"stock\":1}", "name"),
                Arguments.of("{\"name\":\"Keyboard\",\"price\":null,\"stock\":1}", "price"),
                Arguments.of("{\"name\":\"Keyboard\",\"price\":0,\"stock\":1}", "price"),
                Arguments.of("{\"name\":\"Keyboard\",\"price\":-1,\"stock\":1}", "price"),
                Arguments.of("{\"name\":\"Keyboard\",\"price\":10.001,\"stock\":1}", "price"),
                Arguments.of("{\"name\":\"Keyboard\",\"price\":10000000000,\"stock\":1}", "price"),
                Arguments.of("{\"name\":\"Keyboard\",\"price\":10}", "stock"),
                Arguments.of("{\"name\":\"Keyboard\",\"price\":10,\"stock\":null}", "stock"),
                Arguments.of("{\"name\":\"Keyboard\",\"price\":10,\"stock\":-1}", "stock")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"price\":null}", "{\"price\":0}", "{\"price\":-1}",
            "{\"price\":1.234}", "{\"price\":10000000000}"})
    void validatesPriceUpdates(String request) throws Exception {
        mockMvc.perform(patch("/api/products/42/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("price")));
        verifyNoInteractions(productService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{\"name\":\"Keyboard\",\"price\":\"not-a-number\",\"stock\":1}",
            "{\"name\":\"Keyboard\",\"price\":10,\"stock\":1.5}"})
    void rejectsMalformedJsonAndWrongFieldTypes(String request) throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Malformed request"));
        verifyNoInteractions(productService);
    }

    @Test
    void rejectsAnInvalidPathIdentifier() throws Exception {
        mockMvc.perform(get("/api/products/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid request parameter"));
        verifyNoInteractions(productService);
    }

    @Test
    void returnsAProblemWhenTheProductDoesNotExist() throws Exception {
        when(productService.get(404L))
                .thenThrow(new ResourceNotFoundException("Product 404 was not found"));

        mockMvc.perform(get("/api/products/404"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Product 404 was not found"))
                .andExpect(jsonPath("$.instance").value("/api/products/404"));
    }
}
