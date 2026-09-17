package com.stonkbot.commands;

import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;

public final class Options {
    private Options() {
    }

    public static String string(SlashCommandInteraction i, String name) {
        OptionMapping o = i.getOption(name);
        return o == null ? "" : o.getAsString();
    }
}
