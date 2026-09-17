package com.stonkbot.commands;

import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;

public interface BotCommand {
    String name();

    CommandData command();

    CommandResult handle(SlashCommandInteraction interaction) throws Exception;
}
