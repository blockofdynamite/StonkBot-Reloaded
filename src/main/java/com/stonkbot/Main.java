package com.stonkbot;

import com.stonkbot.commands.BalanceCommand;
import com.stonkbot.commands.BotCommand;
import com.stonkbot.commands.BuyCommand;
import com.stonkbot.commands.PortfolioCommand;
import com.stonkbot.commands.QuoteCommand;
import com.stonkbot.commands.ResetCommand;
import com.stonkbot.commands.SellCommand;
import com.stonkbot.db.Database;
import com.stonkbot.market.ChartMarketDataService;
import com.stonkbot.market.CompositeMarketDataService;
import com.stonkbot.market.MarketDataService;
import com.stonkbot.market.YahooMarketDataService;
import com.stonkbot.service.AccountService;
import com.stonkbot.service.TradeService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

public final class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        String token = System.getenv("DISCORD_BOT_TOKEN");
        if (token == null || token.isBlank()) {
            System.err.println("DISCORD_BOT_TOKEN is not set.");
            System.err.println("Copy .env.example to .env, set your token, then run:");
            System.err.println("  export $(grep -v '^#' .env | xargs) && java -jar target/stonkbot-1.0.0.jar");
            System.exit(1);
        }
        String devGuildId = System.getenv("GUILD_ID");

        Database db = new Database(Path.of("data", "stonkbot.db"));
        MarketDataService market = new CompositeMarketDataService(new YahooMarketDataService(), new ChartMarketDataService());
        AccountService accounts = new AccountService(db);
        TradeService trades = new TradeService(accounts, market);

        List<BotCommand> botCommands = List.of(
                new BalanceCommand(accounts, market),
                new QuoteCommand(market),
                new BuyCommand(trades),
                new SellCommand(trades),
                new PortfolioCommand(accounts, market),
                new ResetCommand(accounts));

        StonkBotListener listener = new StonkBotListener(botCommands, accounts, devGuildId);

        log.info("StonkBot starting up...");
        JDABuilder builder = JDABuilder.createDefault(token).addEventListeners(listener);
        JDA jda = builder.build();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            jda.shutdownNow();
            db.close();
        }));
    }
}
