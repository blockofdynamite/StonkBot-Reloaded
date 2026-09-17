package com.stonkbot.commands;

import com.stonkbot.service.TradeService;
import com.stonkbot.util.Money;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public final class BuyCommand implements BotCommand {
    private final TradeService trades;

    public BuyCommand(TradeService trades) {
        this.trades = trades;
    }

    @Override
    public String name() {
        return "buy";
    }

    @Override
    public CommandData command() {
        return Commands.slash("buy", "Buy shares of a stock")
                .addOption(OptionType.STRING, "symbol", "Ticker symbol, e.g. AAPL", true)
                .addOption(OptionType.STRING, "shares", "Number of shares, decimals ok (e.g. 2.5)", true);
    }

    @Override
    public CommandResult handle(SlashCommandInteraction i) throws Exception {
        TradeService.Trade t = trades.buy(i.getGuild().getId(), i.getUser().getId(),
                Options.string(i, "symbol"), Options.string(i, "shares"));
        StringBuilder sb = new StringBuilder();
        sb.append("Bought ").append(Money.shares(t.shares())).append(" shares of **").append(t.name())
                .append("** (`").append(t.symbol()).append("`) at ").append(Money.usd(t.price())).append(" each.\n");
        sb.append("Total: ").append(Money.usd(t.total())).append('\n');
        sb.append("Cash left: ").append(Money.usd(t.cashAfter()));
        return CommandResult.text(sb.toString());
    }
}
