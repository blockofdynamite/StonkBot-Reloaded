package com.stonkbot.commands;

import net.dv8tion.jda.api.components.MessageTopLevelComponent;

import java.util.List;

public record CommandResult(String text, boolean ephemeral, List<MessageTopLevelComponent> components) {
    public static CommandResult text(String text) {
        return new CommandResult(text, false, List.of());
    }

    public static CommandResult ephemeral(String text) {
        return new CommandResult(text, true, List.of());
    }

    public static CommandResult withButtons(String text, List<MessageTopLevelComponent> components) {
        return new CommandResult(text, false, components);
    }
}
