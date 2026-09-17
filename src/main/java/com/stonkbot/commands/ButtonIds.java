package com.stonkbot.commands;

public final class ButtonIds {
    public static final String RESET_PREFIX = "reset:";
    public static final String RESET_CANCEL = "reset_cancel";

    private ButtonIds() {
    }

    public static String resetConfirm(String userId) {
        return RESET_PREFIX + userId;
    }
}
