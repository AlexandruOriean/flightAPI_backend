package com.codecool.flightapi.common.money;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Currency;

/**
 * {@link Money} in requests and responses, e.g. {@code {"amount": 35.00, "currency": "EUR"}}.
 *
 * @param currency ISO 4217 code
 */
public record MoneyDto(
        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal amount,

        @NotNull @Pattern(regexp = "[A-Z]{3}", message = "must be 3 upper-case letters") String currency) {

    /** Validation only: not part of the JSON. */
    @JsonIgnore
    @AssertTrue(message = "must be an ISO 4217 currency code") public boolean isKnownCurrency() {
        return currency == null
                || Currency.getAvailableCurrencies().stream()
                        .anyMatch(known -> known.getCurrencyCode().equals(currency));
    }
}
