package com.stonkbot.market;

import com.stonkbot.market.MarketDataService.Quote;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartMarketDataServiceTest {
    private static final String VALID_JSON = """
            {
              "chart": {
                "result": [{
                  "meta": {
                    "currency": "USD",
                    "symbol": "AAPL",
                    "longName": "Apple Inc.",
                    "regularMarketPrice": 234.56,
                    "previousClose": 233.0
                  }
                }],
                "error": null
              }
            }
            """;

    @Test
    void parsesValidChartResponse() {
        Optional<Quote> q = ChartMarketDataService.parse(VALID_JSON, "AAPL");
        assertTrue(q.isPresent());
        Quote quote = q.get();
        assertEquals("AAPL", quote.symbol());
        assertEquals("Apple Inc.", quote.name());
        assertEquals(0, new BigDecimal("234.56").compareTo(quote.price()));
        assertEquals(0, new BigDecimal("0.67").compareTo(quote.changePercent()));
        assertEquals("USD", quote.currency());
    }

    @Test
    void parsesResponseWithoutLongName() {
        String json = """
                {"chart":{"result":[{"meta":{"symbol":"BTC-USD","shortName":"Bitcoin","currency":"USD","regularMarketPrice":65000.0}}]}}
                """;
        Optional<Quote> q = ChartMarketDataService.parse(json, "BTC-USD");
        assertTrue(q.isPresent());
        assertEquals("Bitcoin", q.get().name());
    }

    @Test
    void fallsBackToChartPreviousClose() {
        String json = """
                {"chart":{"result":[{"meta":{"symbol":"AAPL","longName":"Apple Inc.","currency":"USD","regularMarketPrice":105.0,"chartPreviousClose":100.0}}]}}
                """;
        Optional<Quote> q = ChartMarketDataService.parse(json, "AAPL");
        assertTrue(q.isPresent());
        assertEquals(0, new BigDecimal("5.00").compareTo(q.get().changePercent()));
    }

    @Test
    void parsesErrorResponseAsEmpty() {
        String json = """
                {"chart":{"result":null,"error":{"code":"Not Found","description":"No data found"}}}
                """;
        assertTrue(ChartMarketDataService.parse(json, "NOPE").isEmpty());
    }

    @Test
    void parsesMalformedJsonAsEmpty() {
        assertTrue(ChartMarketDataService.parse("not json", "AAPL").isEmpty());
    }
}
