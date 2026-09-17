package com.stonkbot.commands;

import com.stonkbot.db.Holding;
import com.stonkbot.market.MarketDataService;
import com.stonkbot.market.MarketDataService.Quote;
import com.stonkbot.service.AccountService;
import com.stonkbot.util.Money;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.math.BigDecimal;
import java.util.Optional;

public final class BalanceCommand implements BotCommand {
    private final AccountService accounts;
    private final MarketDataService market;

    public BalanceCommand(AccountService accounts, MarketDataService market) {
        this.accounts = accounts;
        this.market = market;
    }

    @Override
    public String name() {
        return "balance";
    }

    @Override
    public CommandData command() {
        return Commands.slash("balance", "Show your cash, portfolio value, and net worth");
    }

    @Override
    public CommandResult handle(SlashCommandInteraction i) throws Exception {
        String guildId = i.getGuild().getId();
        String userId = i.getUser().getId();
        accounts.ensureAccount(guildId, userId);

        BigDecimal cash = accounts.cash(guildId, userId);
        BigDecimal portfolio = BigDecimal.ZERO;
        for (Holding h : accounts.holdings(guildId, userId)) {
            Optional<Quote> q = market.quote(h.symbol());
            BigDecimal price = q.map(Quote::price).orElse(h.avgCost());
            portfolio = portfolio.add(h.shares().multiply(price));
        }
        BigDecimal total = cash.add(portfolio);
        BigDecimal gain = total.subtract(AccountService.STARTING_BALANCE);

        StringBuilder sb = new StringBuilder();
        sb.append("**").append(i.getUser().getName()).append("'s balance**\n");
        sb.append("Cash: ").append(Money.usd(cash)).append('\n');
        sb.append("Portfolio: ").append(Money.usd(portfolio)).append('\n');
        sb.append("**Net worth: ").append(Money.usd(total)).append("**\n");
        sb.append("Since starting with ").append(Money.usd(AccountService.STARTING_BALANCE))
                .append(", you're ").append(Money.signedUsd(gain))
                .append(" in the ").append(gain.signum() >= 0 ? "green" : "red").append(".");
        return CommandResult.text(sb.toString());
    }
}
