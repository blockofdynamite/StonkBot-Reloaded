package com.stonkbot.db;

import java.math.BigDecimal;

public record Holding(String symbol, BigDecimal shares, BigDecimal avgCost) {}
