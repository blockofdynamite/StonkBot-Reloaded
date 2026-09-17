package com.stonkbot.service;

import com.stonkbot.db.Database;
import com.stonkbot.db.Holding;
import com.stonkbot.market.MarketDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeServiceTest {
    private static final String GUILD = "guild-1";
    private static final String USER = "user-1";

    private static final class FakeMarket implements MarketDataService {
        private final Map<String, BigDecimal> prices = new ConcurrentHashMap<>();

        void price(String symbol, String value) {
            prices.put(symbol.toUpperCase(), new BigDecimal(value));
        }

        @Override
        public Optional<Quote> quote(String symbol) {
            String key = symbol.toUpperCase();
            BigDecimal p = prices.get(key);
            return p == null ? Optional.empty() : Optional.of(new Quote(key, "Test Corp", p, null, "USD"));
        }
    }

    private Database db;
    private AccountService accounts;
    private TradeService trades;
    private FakeMarket market;

    @BeforeEach
    void setUp() {
        db = Database.inMemory();
        market = new FakeMarket();
        accounts = new AccountService(db);
        trades = new TradeService(accounts, market);
    }

    @Test
    void newUsersStartWithOneThousand() {
        accounts.ensureAccount(GUILD, USER);
        assertEquals(0, new BigDecimal("1000").compareTo(accounts.cash(GUILD, USER)));
    }

    @Test
    void buyDeductsCashAndRecordsHolding() {
        market.price("AAPL", "100.00");
        TradeService.Trade trade = trades.buy(GUILD, USER, "AAPL", "10");
        assertEquals(0, BigDecimal.ZERO.compareTo(accounts.cash(GUILD, USER)));
        List<Holding> holdings = accounts.holdings(GUILD, USER);
        assertEquals(1, holdings.size());
        Holding h = holdings.get(0);
        assertEquals(0, new BigDecimal("10").compareTo(h.shares()));
        assertEquals(0, new BigDecimal("100.00").compareTo(h.avgCost()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(trade.total()));
        assertEquals("Test Corp", trade.name());
    }

    @Test
    void buyingMoreWeightsTheAverageCost() {
        market.price("AAPL", "100.00");
        trades.buy(GUILD, USER, "AAPL", "4");
        market.price("AAPL", "110.00");
        trades.buy(GUILD, USER, "AAPL", "4");
        Holding h = accounts.holdings(GUILD, USER).get(0);
        assertEquals(0, new BigDecimal("8").compareTo(h.shares()));
        // (4*100 + 4*110) / 8 = 105
        assertEquals(0, new BigDecimal("105.00").compareTo(h.avgCost()));
        assertEquals(0, new BigDecimal("160.00").compareTo(accounts.cash(GUILD, USER)));
    }

    @Test
    void sellCreditsCashAndKeepsAverageCost() {
        market.price("AAPL", "100.00");
        trades.buy(GUILD, USER, "AAPL", "10");
        market.price("AAPL", "120.00");
        TradeService.Trade trade = trades.sell(GUILD, USER, "AAPL", "5");
        assertEquals(0, new BigDecimal("600").compareTo(accounts.cash(GUILD, USER)));
        Holding h = accounts.holdings(GUILD, USER).get(0);
        assertEquals(0, new BigDecimal("5").compareTo(h.shares()));
        assertEquals(0, new BigDecimal("100.00").compareTo(h.avgCost()));
        assertEquals(0, new BigDecimal("600.00").compareTo(trade.total()));
        assertEquals(0, new BigDecimal("100.00").compareTo(trade.pnl()));
        assertEquals(0, new BigDecimal("20.00").compareTo(trade.pnlPct()));
    }

    @Test
    void sellingEverythingRemovesTheHolding() {
        market.price("AAPL", "100.00");
        trades.buy(GUILD, USER, "AAPL", "10");
        trades.sell(GUILD, USER, "AAPL", "10");
        assertTrue(accounts.holdings(GUILD, USER).isEmpty());
        assertEquals(0, new BigDecimal("1000").compareTo(accounts.cash(GUILD, USER)));
    }

    @Test
    void buyingMoreThanCashAllowsIsRejected() {
        market.price("AAPL", "100.00");
        assertThrows(TradeException.class, () -> trades.buy(GUILD, USER, "AAPL", "11"));
        assertEquals(0, new BigDecimal("1000").compareTo(accounts.cash(GUILD, USER)));
        assertTrue(accounts.holdings(GUILD, USER).isEmpty());
    }

    @Test
    void sellingMoreThanOwnedIsRejected() {
        market.price("AAPL", "100.00");
        trades.buy(GUILD, USER, "AAPL", "5");
        assertThrows(TradeException.class, () -> trades.sell(GUILD, USER, "AAPL", "6"));
        assertEquals(0, new BigDecimal("500").compareTo(accounts.cash(GUILD, USER)));
        assertEquals(0, new BigDecimal("5").compareTo(accounts.holdings(GUILD, USER).get(0).shares()));
    }

    @Test
    void fractionalSharesWork() {
        market.price("AAPL", "100.50");
        TradeService.Trade trade = trades.buy(GUILD, USER, "AAPL", "0.5");
        assertEquals(0, new BigDecimal("50.25").compareTo(trade.total()));
        assertEquals(0, new BigDecimal("949.75").compareTo(accounts.cash(GUILD, USER)));
        assertEquals(0, new BigDecimal("0.5").compareTo(accounts.holdings(GUILD, USER).get(0).shares()));
    }

    @Test
    void unknownSymbolsAreRejected() {
        assertThrows(TradeException.class, () -> trades.buy(GUILD, USER, "NOPE", "1"));
        assertTrue(accounts.holdings(GUILD, USER).isEmpty());
    }

    @Test
    void invalidShareInputsAreRejected() {
        accounts.ensureAccount(GUILD, USER);
        market.price("AAPL", "100.00");
        assertThrows(TradeException.class, () -> trades.buy(GUILD, USER, "AAPL", "0"));
        assertThrows(TradeException.class, () -> trades.buy(GUILD, USER, "AAPL", "-1"));
        assertThrows(TradeException.class, () -> trades.buy(GUILD, USER, "AAPL", "abc"));
        assertThrows(TradeException.class, () -> trades.buy(GUILD, USER, "AAPL", "0.12345"));
        assertTrue(accounts.holdings(GUILD, USER).isEmpty());
        assertEquals(0, new BigDecimal("1000").compareTo(accounts.cash(GUILD, USER)));
    }

    @Test
    void resetRestoresTheStartingBalance() {
        market.price("AAPL", "100.00");
        trades.buy(GUILD, USER, "AAPL", "10");
        accounts.reset(GUILD, USER);
        assertEquals(0, new BigDecimal("1000").compareTo(accounts.cash(GUILD, USER)));
        assertTrue(accounts.holdings(GUILD, USER).isEmpty());
    }

    @Test
    void usersInDifferentGuildsAreIndependent() {
        market.price("AAPL", "100.00");
        trades.buy(GUILD, USER, "AAPL", "1");
        accounts.ensureAccount("guild-2", USER);
        assertEquals(0, new BigDecimal("1000").compareTo(accounts.cash("guild-2", USER)));
        assertTrue(accounts.holdings("guild-2", USER).isEmpty());
    }
}
