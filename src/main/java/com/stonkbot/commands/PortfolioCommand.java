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
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

public final class PortfolioCommand implements BotCommand {
    private final AccountService accounts;
    private final MarketDataService market;

    public PortfolioCommand(AccountService accounts, MarketDataService market) {
        this.accounts = accounts;
        this.market = market;
    }

    @Override
    public String name() {
        return "portfolio";
    }

    @Override
    public CommandData command() {
        return Commands.slash("portfolio", "List your holdings and their performance");
    }

    @Override
    public CommandResult handle(SlashCommandInteraction i) throws Exception {
        String guildId = i.getGuild().getId();
        String userId = i.getUser().getId();
        accounts.ensureAccount(guildId, userId);

        List<Holding> holdings = accounts.holdings(guildId, userId);
        if (holdings.isEmpty()) {
            return CommandResult.text("You don't own any stonks yet. Try `/buy`!");
        }

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        StringBuilder sb = new StringBuilder("**Portfolio**\n");
        for (Holding h : holdings) {
            Optional<Quote> q = market.quote(h.symbol());
            BigDecimal cost = h.shares().multiply(h.avgCost());
            totalCost = totalCost.add(cost);
            if (q.isPresent()) {
                BigDecimal price = q.get().price();
                BigDecimal value = h.shares().multiply(price);
                totalValue = totalValue.add(value);
                BigDecimal pnl = value.subtract(cost);
                BigDecimal pnlPct = cost.signum() == 0
                        ? BigDecimal.ZERO
                        : pnl.multiply(BigDecimal.valueOf(100)).divide(cost, 2, RoundingMode.HALF_UP);
                sb.append("`").append(h.symbol()).append("`\n");
                sb.append("  ").append(Money.shares(h.shares())).append(" shares @ ")
                        .append(Money.usd(h.avgCost())).append(" avg\n");
                sb.append("  Last: ").append(Money.usd(price))
                        .append(" | Value: ").append(Money.usd(value))
                        .append(" | P/L: ").append(Money.signedUsd(pnl))
                        .append(" (").append(Money.pct(pnlPct)).append(")\n");
            } else {
                sb.append("`").append(h.symbol()).append("`\n");
                sb.append("  ").append(Money.shares(h.shares())).append(" shares @ ")
                        .append(Money.usd(h.avgCost())).append(" avg (quote unavailable)\n");
            }
        }
        BigDecimal totalPnl = totalValue.subtract(totalCost);
        sb.append("---\n");
        sb.append("Total cost: ").append(Money.usd(totalCost))
                .append(" | Total value: ").append(Money.usd(totalValue))
                .append(" | P/L: ").append(Money.signedUsd(totalPnl));
        return CommandResult.text(sb.toString());
    }
}
