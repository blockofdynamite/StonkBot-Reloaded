# StonkBot
StonkBot, but for Discord.

A Discord bot where users virtually invest in the stock market. Every user starts
with **$1,000** in each server and can trade live US stocks with fractional shares.
Market data comes from [financequotes-api.com](https://financequotes-api.com)
(the `yahoofinance-api` Java library for Yahoo Finance), with a direct Yahoo v8 chart
endpoint as an automatic fallback.

## WARNING: This is a clanker project! I barely touched this code. I am not proud of this code. This is not my code. Use at your own risk!
## Commands
| Command | What it does |
|---|---|
| `/balance` | Cash, portfolio value, and net worth |
| `/quote` | Latest price and day change for a ticker |
| `/buy` | Buy shares (decimals ok, e.g. `2.5`) |
| `/sell` | Sell shares, with P/L on the sale |
| `/portfolio` | All holdings with avg cost, value, and P/L |
| `/reset` | Wipe holdings and go back to $1,000 (confirm button) |

Balances are per server: each guild gets its own independent economy, and users are
auto-provisioned with $1,000 the first time they use any command in a server.
State lives in a SQLite file at `data/stonkbot.db`.

## Prerequisites
- JDK 17
- Maven (or just use the included wrapper, `./mvnw`)

## 1. Create the Discord application
1. Go to the [Discord Developer Portal](https://discord.com/developers/applications) and click **New Application**.
2. Open the **Bot** tab and click **Reset Token** — copy the token (you'll need it below).
   No privileged gateway intents are required.
3. Invite the bot to your server. Replace `YOUR_CLIENT_ID` with the **Application ID**
   from the **General Information** tab:

   ```
   https://discord.com/oauth2/authorize?client_id=YOUR_CLIENT_ID&permissions=0&scope=bot%20applications.commands
   ```

## 2. Configure and run
```sh
cp .env.example .env
# edit .env and set DISCORD_BOT_TOKEN

export $(grep -v '^#' .env | xargs)
./mvnw -q package
java -jar target/stonkbot-1.0.0.jar
```

### Development tip
Set `GUILD_ID` in `.env` to your test server's ID (Developer Mode > right-click the
server > Copy ID). Commands are then registered in that server only and appear
instantly. When `GUILD_ID` is unset, commands are registered globally, which can
take a few minutes to propagate.

## Project layout
```
src/main/java/com/stonkbot/
├── Main.java                     # entry point, wiring
├── StonkBotListener.java         # slash command + button event handling
├── commands/                     # one class per slash command
├── market/
│   ├── MarketDataService.java    # quote interface (Quote record)
│   ├── YahooMarketDataService.java    # financequotes-api.com, 30s cache
│   ├── ChartMarketDataService.java    # direct Yahoo v8 chart fallback
│   └── CompositeMarketDataService.java
├── db/                           # SQLite schema + connection
├── service/
│   ├── AccountService.java       # accounts, cash, holdings, reset
│   └── TradeService.java         # buy/sell, atomic transactions, validation
└── util/Money.java               # currency/percent/share formatting
```

## Notes
- Trades are executed atomically (SQLite transactions) with weighted average cost
  tracking; all math uses `BigDecimal`.
- Quotes are cached for 30 seconds per symbol to keep request volume to Yahoo low.
- If the financequotes-api library can't reach Yahoo (e.g. HTTP 429), the bot
  automatically falls back to Yahoo's v8 chart endpoint for that symbol.
- `data/` and `.env` are gitignored.
