package com.stonkbot.service;

import com.stonkbot.db.Holding;
import com.stonkbot.market.MarketDataService;
import com.stonkbot.market.MarketDataService.Quote;
import com.stonkbot.util.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class TradeService {

    public record Trade(String symbol, String name, BigDecimal shares, BigDecimal price, BigDecimal total,
                        BigDecimal cashAfter, BigDecimal pnl, BigDecimal pnlPct) {}

    private static final Pattern SYMBOL = Pattern.compile("^[A-Z0-9.\\-]{1,15}$");

    private final AccountService accounts;
    private final MarketDataService market;

    public TradeService(AccountService accounts, MarketDataService market) {
        this.accounts = accounts;
        this.market = market;
    }

    public synchronized Trade buy(String guildId, String userId, String rawSymbol, String rawShares) {
        String symbol = validateSymbol(rawSymbol);
        BigDecimal shares = validateShares(rawShares);
        Quote quote = fetchQuote(symbol);

        accounts.ensureAccount(guildId, userId);
        BigDecimal total = quote.price().multiply(shares);
        BigDecimal cash = accounts.cash(guildId, userId);
        if (cash.compareTo(total) < 0) {
            throw new TradeException(String.format(
                    "Not enough cash. You have %s, but %s shares of %s cost %s.",
                    Money.usd(cash), shares.toPlainString(), symbol, Money.usd(total)));
        }
        accounts.applyBuy(guildId, userId, symbol, shares, quote.price());
        return new Trade(symbol, quote.name(), shares, quote.price(), total, accounts.cash(guildId, userId), null, null);
    }

    public synchronized Trade sell(String guildId, String userId, String rawSymbol, String rawShares) {
        String symbol = validateSymbol(rawSymbol);
        BigDecimal shares = validateShares(rawShares);
        Quote quote = fetchQuote(symbol);

        accounts.ensureAccount(guildId, userId);
        Optional<Holding> holding = accounts.findHolding(guildId, userId, symbol);
        if (holding.isEmpty() || holding.get().shares().compareTo(shares) < 0) {
            String have = holding.map(h -> h.shares().toPlainString()).orElse("0");
            throw new TradeException("You only own " + have + " shares of " + symbol
                    + ", can't sell " + shares.toPlainString() + ".");
        }
        BigDecimal avgCost = holding.get().avgCost();
        BigDecimal total = quote.price().multiply(shares);
        BigDecimal cost = avgCost.multiply(shares);
        BigDecimal pnl = total.subtract(cost);
        BigDecimal pnlPct = cost.signum() == 0
                ? BigDecimal.ZERO
                : pnl.multiply(BigDecimal.valueOf(100)).divide(cost, 2, RoundingMode.HALF_UP);
        accounts.applySell(guildId, userId, symbol, shares, quote.price());
        return new Trade(symbol, quote.name(), shares, quote.price(), total, accounts.cash(guildId, userId), pnl, pnlPct);
    }

    private Quote fetchQuote(String symbol) {
        return market.quote(symbol).orElseThrow(() ->
                new TradeException("Couldn't find a quote for `" + symbol + "`. Try a US ticker like `AAPL` or `TSLA`."));
    }

    private static String validateSymbol(String raw) {
        String symbol = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!SYMBOL.matcher(symbol).matches()) {
            throw new TradeException("`" + raw + "` doesn't look like a ticker symbol. Try something like `AAPL`.");
        }
        return symbol;
    }

    private static BigDecimal validateShares(String raw) {
        BigDecimal shares;
        try {
            shares = new BigDecimal(raw == null ? "" : raw.trim());
        } catch (NumberFormatException ex) {
            throw new TradeException("`" + raw + "` isn't a valid number of shares.");
        }
        if (shares.signum() <= 0) {
            throw new TradeException("Shares must be greater than zero.");
        }
        if (shares.scale() > 4) {
            throw new TradeException("Use at most 4 decimal places for shares.");
        }
        return shares;
    }
}
