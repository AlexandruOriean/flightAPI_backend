package com.codecool.flightapi.common.money;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.Currency;

/** An amount in a currency. Embedded in entities; the columns are named by the embedding entity. */
@Embeddable
public record Money(BigDecimal amount, Currency currency) {}
