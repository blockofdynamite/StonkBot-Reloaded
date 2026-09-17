package com.stonkbot.commands;

import com.stonkbot.service.TradeService;
import com.stonkbot.util.Money;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public final class SellCommand implements BotCommand {
    private final TradeService trades;

    public SellCommand(TradeService trades) {
        this.trades = trades;
    }

    @Override
    public String name() {
        return "sell";
    }

    @Override
    public CommandData command() {
        return Commands.slash("sell", "Sell shares of a stock you own")
                .addOption(OptionType.STRING, "symbol", "Ticker symbol, e.g. AAPL", true)
                .addOption(OptionType.STRING, "shares", "Number of shares to sell, decimals ok", true);
    }

    @Override
    public CommandResult handle(SlashCommandInteraction i) throws Exception {
        TradeService.Trade t = trades.sell(i.getGuild().getId(), i.getUser().getId(),
                Options.string(i, "symbol"), Options.string(i, "shares"));
        StringBuilder sb = new StringBuilder();
        sb.append("Sold ").append(Money.shares(t.shares())).append(" shares of **").append(t.name())
                .append("** (`").append(t.symbol()).append("`) at ").append(Money.usd(t.price())).append(" each.\n");
        sb.append("Proceeds: ").append(Money.usd(t.total())).append('\n');
        sb.append("P/L on sale: ").append(Money.signedUsd(t.pnl())).append(" (").append(Money.pct(t.pnlPct())).append(")\n");
        sb.append("Cash: ").append(Money.usd(t.cashAfter()));
        return CommandResult.text(sb.toString());
    }
}
