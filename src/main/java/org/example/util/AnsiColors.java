package org.example.util;

import org.fusesource.jansi.Ansi;
import static org.fusesource.jansi.Ansi.ansi;

public final class AnsiColors {

    public static final Ansi.Color BLACK = Ansi.Color.BLACK;
    public static final Ansi.Color RED = Ansi.Color.RED;
    public static final Ansi.Color GREEN = Ansi.Color.GREEN;
    public static final Ansi.Color YELLOW = Ansi.Color.YELLOW;
    public static final Ansi.Color BLUE = Ansi.Color.BLUE;
    public static final Ansi.Color MAGENTA = Ansi.Color.MAGENTA;
    public static final Ansi.Color CYAN = Ansi.Color.CYAN;
    public static final Ansi.Color WHITE = Ansi.Color.WHITE;
    public static final Ansi.Color DEFAULT = Ansi.Color.DEFAULT;

    public static String colorize(String text, Ansi.Color color) {
        return ansi().fg(color).a(text).reset().toString();
    }

    public static String green(String text) {
        return colorize(text, GREEN);
    }

    public static String yellow(String text) {
        return colorize(text, YELLOW);
    }

    public static String red(String text) {
        return colorize(text, RED);
    }

    public static String cyan(String text) {
        return colorize(text, CYAN);
    }

    public static String magenta(String text) {
        return colorize(text, MAGENTA);
    }
}

