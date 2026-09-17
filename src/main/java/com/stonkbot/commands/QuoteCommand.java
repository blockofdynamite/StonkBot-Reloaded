package com.stonkbot.commands;

import com.stonkbot.market.MarketDataService;
import com.stonkbot.market.MarketDataService.Quote;
import com.stonkbot.util.Money;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.Locale;
import java.util.Optional;

public final class QuoteCommand implements BotCommand {
    private final MarketDataService market;

    public QuoteCommand(MarketDataService market) {
        this.market = market;
    }

    @Override
    public String name() {
        return "quote";
    }

    @Override
    public CommandData command() {
        return Commands.slash("quote", "Get the latest price for a stock")
                .addOption(OptionType.STRING, "symbol", "Ticker symbol, e.g. AAPL", true);
    }

    @Override
    public CommandResult handle(SlashCommandInteraction i) throws Exception {
        String symbol = Options.string(i, "symbol").trim().toUpperCase(Locale.ROOT);
        Optional<Quote> q = market.quote(symbol);
        if (q.isEmpty()) {
            return CommandResult.ephemeral("Couldn't find a quote for `" + symbol + "`. Try a US ticker like `AAPL` or `TSLA`.");
        }
        Quote quote = q.get();
        StringBuilder sb = new StringBuilder();
        sb.append("**").append(quote.name()).append("** (`").append(quote.symbol()).append("`)\n");
        sb.append("Price: ").append(Money.usd(quote.price())).append(' ').append(quote.currency());
        if (quote.changePercent() != null) {
            sb.append('\n').append("Day change: ").append(Money.pct(quote.changePercent()));
        }
        return CommandResult.text(sb.toString());
    }
}
