package com.stonkbot.commands;

import com.stonkbot.service.AccountService;
import com.stonkbot.util.Money;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;

import java.util.List;

public final class ResetCommand implements BotCommand {
    private final AccountService accounts;

    public ResetCommand(AccountService accounts) {
        this.accounts = accounts;
    }

    @Override
    public String name() {
        return "reset";
    }

    @Override
    public CommandData command() {
        return Commands.slash("reset", "Reset your account to the $1,000 starting balance (wipes holdings)");
    }

    @Override
    public CommandResult handle(SlashCommandInteraction i) {
        List<MessageTopLevelComponent> buttons = List.of(
                ActionRow.of(
                        Button.primary(ButtonIds.resetConfirm(i.getUser().getId()), "Confirm reset"),
                        Button.danger(ButtonIds.RESET_CANCEL, "Cancel")));
        return CommandResult.withButtons(
                "This erases all your holdings and sets your cash back to "
                        + Money.usd(AccountService.STARTING_BALANCE) + ".",
                buttons);
    }
}
