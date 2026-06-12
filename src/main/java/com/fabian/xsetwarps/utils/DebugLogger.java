package com.fabian.xsetwarps.utils;

import com.fabian.xsetwarps.XSetWarps;
import org.bukkit.Bukkit;

/**
 * Static debug logging utility.
 * <p>
 * All debug output is gated behind the {@code debug} config key
 * (default {@code false}) so it has zero overhead in production.
 */
public final class DebugLogger {

    private static final String PREFIX = "&8[&bDEBUG&8]&r ";

    private DebugLogger() {
        // static utility – no instances
    }

    /**
     * Returns {@code true} when the debug flag is enabled in config.yml.
     * Includes null-safety in case the plugin or config is not yet available.
     */
    private static boolean isDebugEnabled() {
        try {
            XSetWarps plugin = XSetWarps.getInstance();
            if (plugin == null) return false;
            if (plugin.getConfig() == null) return false;
            return plugin.getConfig().getBoolean("debug", false);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Log a simple debug message.
     *
     * @param message the message to log
     */
    public static void debug(String message) {
        if (isDebugEnabled()) {
            Bukkit.getConsoleSender().sendMessage(ColorUtils.translateColors(PREFIX + message));
        }
    }

    /**
     * Log a debug message with a category tag.
     *
     * @param category a short label (e.g. "WarpManager", "GUI")
     * @param message  the message to log
     */
    public static void debug(String category, String message) {
        if (isDebugEnabled()) {
            Bukkit.getConsoleSender().sendMessage(
                    ColorUtils.translateColors(PREFIX + "&f[&f" + category + "&f]&r &7" + message));
        }
    }

    /**
     * Log a debug message with a category tag and an associated throwable stack trace.
     *
     * @param category  a short label
     * @param message   the message to log
     * @param throwable the exception whose stack trace is appended
     */
    public static void debug(String category, String message, Throwable throwable) {
        if (isDebugEnabled()) {
            Bukkit.getConsoleSender().sendMessage(
                    ColorUtils.translateColors(PREFIX + "&f[&f" + category + "&f]&r &7" + message));
            throwable.printStackTrace();
        }
    }
}