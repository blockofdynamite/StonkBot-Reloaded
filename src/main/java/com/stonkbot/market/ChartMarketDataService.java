package com.stonkbot.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ChartMarketDataService implements MarketDataService {
    private static final Logger log = LoggerFactory.getLogger(ChartMarketDataService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CHART_URL = "https://query1.finance.yahoo.com/v8/finance/chart/%s?range=1d&interval=1d";
    private static final long CACHE_TTL_MS = 30_000L;

    private record Cached(Quote quote, long fetchedAt) {}

    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    @Override
    public synchronized Optional<Quote> quote(String symbol) {
        String key = symbol.toUpperCase(Locale.ROOT);
        Cached cached = cache.get(key);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.fetchedAt() < CACHE_TTL_MS) {
            return Optional.of(cached.quote());
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(String.format(CHART_URL, URLEncoder.encode(key, StandardCharsets.UTF_8))).openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; StonkBot/1.0)");
            int status = connection.getResponseCode();
            if (status != 200) {
                log.warn("Chart endpoint returned HTTP {} for {}", status, key);
                return cached != null ? Optional.of(cached.quote()) : Optional.empty();
            }
            try (InputStream in = connection.getInputStream()) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                Optional<Quote> quote = parse(json, key);
                if (quote.isPresent()) {
                    cache.put(key, new Cached(quote.get(), now));
                }
                return quote;
            }
        } catch (IOException ex) {
            log.warn("Failed to fetch chart quote for {}", key, ex);
            return cached != null ? Optional.of(cached.quote()) : Optional.empty();
        }
    }

    static Optional<Quote> parse(String json, String symbol) {
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode meta = root.path("chart").path("result").path(0).path("meta");
            if (meta.isMissingNode()) {
                return Optional.empty();
            }
            JsonNode priceNode = meta.path("regularMarketPrice");
            if (priceNode.isMissingNode() || priceNode.isNull()) {
                return Optional.empty();
            }
            BigDecimal price = BigDecimal.valueOf(priceNode.asDouble());
            String name = meta.path("longName").asText("");
            if (name.isBlank()) {
                name = meta.path("shortName").asText(symbol);
            }
            if (name.isBlank()) {
                name = symbol;
            }
            String currency = meta.path("currency").asText("USD");
            BigDecimal changePercent = null;
            JsonNode prevNode = meta.path("previousClose");
            if (prevNode.isMissingNode() || prevNode.isNull()) {
                prevNode = meta.path("chartPreviousClose");
            }
            if (prevNode.isNumber() && prevNode.asDouble() != 0.0) {
                BigDecimal prev = BigDecimal.valueOf(prevNode.asDouble());
                changePercent = price.subtract(prev).multiply(BigDecimal.valueOf(100))
                        .divide(prev, 2, RoundingMode.HALF_UP);
            }
            return Optional.of(new Quote(symbol, name, price, changePercent, currency));
        } catch (IOException ex) {
            log.warn("Failed to parse chart response for {}", symbol, ex);
            return Optional.empty();
        }
    }
}
