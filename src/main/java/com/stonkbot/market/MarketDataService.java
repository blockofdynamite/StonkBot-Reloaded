package com.stonkbot.market;

import java.math.BigDecimal;
import java.util.Optional;

public interface MarketDataService {

    record Quote(String symbol, String name, BigDecimal price, BigDecimal changePercent, String currency) {}

    Optional<Quote> quote(String symbol);
}
