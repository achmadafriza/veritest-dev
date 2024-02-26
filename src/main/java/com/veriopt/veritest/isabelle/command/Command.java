package com.veriopt.veritest.isabelle.command;

public enum Command {
    AUTO("."),
    SLEDGEHAMMER("""
            sledgehammer
            sorry
            """),
    NITPICK("""
            nitpick
            sorry
            """);

    private final String isabelleCommand;

    Command(String isabelleCommand) {
        this.isabelleCommand = isabelleCommand;
    }

    @Override
    public String toString() {
        return this.isabelleCommand;
    }
}
