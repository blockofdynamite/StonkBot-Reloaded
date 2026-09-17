package com.stonkbot;

import com.stonkbot.commands.BotCommand;
import com.stonkbot.commands.ButtonIds;
import com.stonkbot.commands.CommandResult;
import com.stonkbot.service.AccountService;
import com.stonkbot.service.TradeException;
import com.stonkbot.util.Money;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StonkBotListener extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(StonkBotListener.class);

    private final Map<String, BotCommand> handlers = new HashMap<>();
    private final List<CommandData> commands = new ArrayList<>();
    private final AccountService accounts;
    private final String devGuildId;

    public StonkBotListener(List<BotCommand> botCommands, AccountService accounts, String devGuildId) {
        for (BotCommand c : botCommands) {
            handlers.put(c.name(), c);
            commands.add(c.command());
        }
        this.accounts = accounts;
        this.devGuildId = (devGuildId == null || devGuildId.isBlank()) ? null : devGuildId.trim();
    }

    public List<CommandData> commands() {
        return List.copyOf(commands);
    }

    @Override
    public void onReady(ReadyEvent event) {
        if (devGuildId != null) {
            Guild guild = event.getJDA().getGuildById(devGuildId);
            if (guild == null) {
                log.error("Dev guild {} not found; is the bot invited to it?", devGuildId);
            } else {
                guild.updateCommands().addCommands(commands).queue(
                        registered -> log.info("Registered {} slash commands in dev guild", registered.size()),
                        err -> log.error("Failed to register slash commands in dev guild", err));
            }
        } else {
            event.getJDA().updateCommands().addCommands(commands).queue(
                    registered -> log.info("Registered {} global slash commands", registered.size()),
                    err -> log.error("Failed to register global slash commands", err));
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        BotCommand handler = handlers.get(event.getName());
        if (handler == null) {
            return;
        }
        if (event.getGuild() == null) {
            event.reply("This bot only works inside a server.").setEphemeral(true).queue();
            return;
        }
        event.deferReply().queue();
        try {
            send(event, handler.handle(event));
        } catch (TradeException ex) {
            send(event, CommandResult.ephemeral(ex.getMessage()));
        } catch (Exception ex) {
            log.error("Slash command {} failed", event.getName(), ex);
            send(event, CommandResult.ephemeral("Something went wrong while processing that. Please try again."));
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (id.equals(ButtonIds.RESET_CANCEL)) {
            event.editMessage("Reset cancelled. Your stonks are safe.").queue();
            return;
        }
        if (!id.startsWith(ButtonIds.RESET_PREFIX)) {
            return;
        }
        String ownerId = id.substring(ButtonIds.RESET_PREFIX.length());
        if (!ownerId.equals(event.getUser().getId())) {
            event.reply("That confirmation belongs to someone else.").setEphemeral(true).queue();
            return;
        }
        if (event.getGuild() == null) {
            event.editMessage("This only works inside a server.").queue();
            return;
        }
        try {
            accounts.reset(event.getGuild().getId(), ownerId);
            event.editMessage(
                    "Account reset. You're back to " + Money.usd(AccountService.STARTING_BALANCE) + " cash and zero holdings.")
                    .queue();
        } catch (Exception ex) {
            log.error("Reset failed for user {}", ownerId, ex);
            event.editMessage("Reset failed. Please try again.").queue();
        }
    }

    private void send(SlashCommandInteractionEvent event, CommandResult result) {
        MessageCreateBuilder builder = new MessageCreateBuilder().addContent(result.text());
        if (!result.components().isEmpty()) {
            builder.addComponents(result.components());
        }
        event.getHook().setEphemeral(result.ephemeral()).sendMessage(builder.build()).queue();
    }
}
