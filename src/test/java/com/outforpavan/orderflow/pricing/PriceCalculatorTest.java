package com.outforpavan.orderflow.pricing;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceCalculatorTest {
    private final PriceCalculator calculator = new PriceCalculator();

    @Test
    void chargesFivePercentPriorityOnTheDiscountedAmount() {
        PriceBreakdown price = calculator.calculate(new BigDecimal("1000.00"), 1,
                new BigDecimal("10"), ServiceLevel.PRIORITY_5);

        assertThat(price.getSubtotal()).isEqualByComparingTo("1000.00");
        assertThat(price.getDiscountPercent()).isEqualByComparingTo("10");
        assertThat(price.getDiscountAmount()).isEqualByComparingTo("100.00");
        assertThat(price.getPrioritySurchargePercent()).isEqualByComparingTo("5");
        assertThat(price.getPrioritySurchargeAmount()).isEqualByComparingTo("45.00");
        assertThat(price.getTotal()).isEqualByComparingTo("945.00");
        assertThat(price.getServiceLevel()).isEqualTo(ServiceLevel.PRIORITY_5);
    }

    @Test
    void chargesTenPercentPriorityForTheWholeQuantity() {
        PriceBreakdown price = calculator.calculate(new BigDecimal("1000.00"), 3,
                new BigDecimal("10"), ServiceLevel.PRIORITY_10);

        assertThat(price.getSubtotal()).isEqualByComparingTo("3000.00");
        assertThat(price.getDiscountAmount()).isEqualByComparingTo("300.00");
        assertThat(price.getPrioritySurchargePercent()).isEqualByComparingTo("10");
        assertThat(price.getPrioritySurchargeAmount()).isEqualByComparingTo("270.00");
        assertThat(price.getTotal()).isEqualByComparingTo("2970.00");
    }

    @Test
    void standardServiceHasNoPriorityCharge() {
        PriceBreakdown price = calculator.calculate(new BigDecimal("1000.00"), 1,
                new BigDecimal("10"), ServiceLevel.STANDARD);

        assertThat(price.getPrioritySurchargeAmount()).isEqualByComparingTo("0.00");
        assertThat(price.getTotal()).isEqualByComparingTo("900.00");
    }

    @Test
    void nonDiscountedProductsCanStillHaveAPriorityCharge() {
        PriceBreakdown price = calculator.calculate(new BigDecimal("1000.00"), 1,
                BigDecimal.ZERO, ServiceLevel.PRIORITY_5);

        assertThat(price.getDiscountAmount()).isEqualByComparingTo("0.00");
        assertThat(price.getPrioritySurchargeAmount()).isEqualByComparingTo("50.00");
        assertThat(price.getTotal()).isEqualByComparingTo("1050.00");
    }

    @Test
    void roundsPerLineBeforeApplyingTheFee() {
        PriceBreakdown price = calculator.calculate(new BigDecimal("0.05"), 3,
                new BigDecimal("10"), ServiceLevel.PRIORITY_5);

        assertThat(price.getSubtotal()).isEqualByComparingTo("0.15");
        assertThat(price.getDiscountAmount()).isEqualByComparingTo("0.02");
        assertThat(price.getPrioritySurchargeAmount()).isEqualByComparingTo("0.01");
        assertThat(price.getTotal()).isEqualByComparingTo("0.14");
        assertThat(price.getTotal().scale()).isEqualTo(2);
    }

    @Test
    void fullDiscountAlsoMakesThePercentageBasedPriorityFeeZero() {
        PriceBreakdown price = calculator.calculate(new BigDecimal("10.00"), 2,
                new BigDecimal("100"), ServiceLevel.PRIORITY_10);

        assertThat(price.getDiscountAmount()).isEqualByComparingTo("20.00");
        assertThat(price.getPrioritySurchargeAmount()).isEqualByComparingTo("0.00");
        assertThat(price.getTotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void permitsTrailingZerosWithoutAcceptingFractionalCents() {
        PriceBreakdown price = calculator.calculate(new BigDecimal("10.0000"), 2,
                BigDecimal.ZERO, ServiceLevel.STANDARD);

        assertThat(price.getSubtotal()).isEqualTo(new BigDecimal("20.00"));
        assertThat(price.getTotal()).isEqualTo(new BigDecimal("20.00"));
    }

    @Test
    void supportsAFractionalDiscountPercentage() {
        PriceBreakdown price = calculator.calculate(new BigDecimal("100.00"), 1,
                new BigDecimal("2.5000"), ServiceLevel.STANDARD);

        assertThat(price.getDiscountPercent()).isEqualTo(new BigDecimal("2.50"));
        assertThat(price.getDiscountAmount()).isEqualByComparingTo("2.50");
        assertThat(price.getTotal()).isEqualByComparingTo("97.50");
    }

    @Test
    void leavesTheCatalogPriceAndEarlierCalculationUnchanged() {
        BigDecimal catalogPrice = new BigDecimal("1000.00");
        PriceBreakdown standard = calculator.calculate(catalogPrice, 1,
                new BigDecimal("10"), ServiceLevel.STANDARD);
        calculator.calculate(catalogPrice, 1, new BigDecimal("10"), ServiceLevel.PRIORITY_10);

        assertThat(catalogPrice).isEqualTo(new BigDecimal("1000.00"));
        assertThat(standard.getSubtotal()).isEqualByComparingTo("1000.00");
        assertThat(standard.getTotal()).isEqualByComparingTo("900.00");
        assertThat(standard.getServiceLevel()).isEqualTo(ServiceLevel.STANDARD);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-0.01", "0.001", "12.345"})
    void rejectsInvalidUnitPrices(String unitPrice) {
        assertThatThrownBy(() -> calculator.calculate(new BigDecimal(unitPrice), 1,
                BigDecimal.ZERO, ServiceLevel.STANDARD)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsInvalidQuantities(int quantity) {
        assertThatThrownBy(() -> calculator.calculate(BigDecimal.ONE, quantity,
                BigDecimal.ZERO, ServiceLevel.STANDARD)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "100.01", "0.001"})
    void rejectsOutOfRangeDiscounts(String discount) {
        assertThatThrownBy(() -> calculator.calculate(BigDecimal.ONE, 1,
                new BigDecimal(discount), ServiceLevel.STANDARD)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingInputs() {
        assertThatThrownBy(() -> calculator.calculate(null, 1, BigDecimal.ZERO, ServiceLevel.STANDARD))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(BigDecimal.ONE, 1, null, ServiceLevel.STANDARD))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(BigDecimal.ONE, 1, BigDecimal.ZERO, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void selectedCampaignDiscountsOnlyEligibleProducts() {
        ProductDiscountPolicy policy = new ProductDiscountPolicy(false,
                new HashSet<Long>(Arrays.asList(101L, 202L)));

        assertThat(policy.getDiscountPercent(101L)).isEqualByComparingTo("10");
        assertThat(policy.getDiscountPercent(202L)).isEqualByComparingTo("10");
        assertThat(policy.getDiscountPercent(303L)).isEqualByComparingTo("0");
    }

    @Test
    void allProductsCampaignDoesNotNeedSelectedIds() {
        ProductDiscountPolicy policy = new ProductDiscountPolicy(true, Collections.<Long>emptySet());

        assertThat(policy.getDiscountPercent(101L)).isEqualByComparingTo("10");
        assertThat(policy.getDiscountPercent(202L)).isEqualByComparingTo("10");
    }

    @Test
    void emptySelectedCampaignDoesNotDiscountAnyProduct() {
        ProductDiscountPolicy policy = new ProductDiscountPolicy(false, Collections.<Long>emptySet());
        assertThat(policy.getDiscountPercent(101L)).isEqualByComparingTo("0");
    }

    @Test
    void policyCopiesTheProvidedSet() {
        Set<Long> selectedIds = new HashSet<Long>(Collections.singleton(101L));
        ProductDiscountPolicy policy = new ProductDiscountPolicy(false, selectedIds);
        selectedIds.clear();
        selectedIds.add(202L);

        assertThat(policy.getDiscountPercent(101L)).isEqualByComparingTo("10");
        assertThat(policy.getDiscountPercent(202L)).isEqualByComparingTo("0");
    }

    @Test
    void rejectsInvalidCampaignIdsAndMissingSet() {
        assertThatThrownBy(() -> new ProductDiscountPolicy(false, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProductDiscountPolicy(true, Collections.<Long>singleton(null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProductDiscountPolicy(false, Collections.singleton(0L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProductDiscountPolicy(false, Collections.singleton(-1L)))
                .isInstanceOf(IllegalArgumentException.class);

        ProductDiscountPolicy policy = new ProductDiscountPolicy(true, Collections.<Long>emptySet());
        assertThatThrownBy(() -> policy.getDiscountPercent(0L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.getDiscountPercent(-1L)).isInstanceOf(IllegalArgumentException.class);
    }
}
