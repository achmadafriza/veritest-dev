package com.veriopt.veritest.isabelle;

import java.util.Locale;

public enum TaskType {
    SESSION_START, SESSION_STOP, USE_THEORIES;

    @Override
    public String toString() {
        return super.toString().toLowerCase(Locale.ROOT);
    }
}
