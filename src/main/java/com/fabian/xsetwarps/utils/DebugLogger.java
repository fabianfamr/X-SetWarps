package com.fabian.xsetwarps.utils;

import com.fabian.xsetwarps.XSetWarps;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * DebugLogger - Static utility for debug logging with two modes:
 * <p>
 * - Config debug (debug: true in config.yml): messages go to CONSOLE only.
 * - Command debug (/xsetwarp debug): messages go to the PLAYER who toggled it.
 * <p>
 * When both are active, only the player receives command-triggered debug messages.
 * Config debug always outputs to console independently.
 */
public final class DebugLogger {

    private static final String PREFIX = "&8[&bDEBUG&8]&r &7";

    private DebugLogger() {
        // static utility – no instances
    }

    /**
     * Checks if debug mode is active (either via config or via command).
     */
    private static boolean isDebugEnabled() {
        try {
            XSetWarps plugin = XSetWarps.getInstance();
            if (plugin == null) return false;
            if (plugin.getConfig() == null) return false;
            boolean configDebug = plugin.getConfig().getBoolean("debug", false);
            return configDebug || plugin.debugPlayer != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if config-based debug is active (console output).
     */
    private static boolean isConfigDebug() {
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
     * Gets the player who enabled debug via command, or null.
     */
    private static Player getDebugPlayer() {
        XSetWarps plugin = XSetWarps.getInstance();
        if (plugin == null || plugin.debugPlayer == null) return null;
        return Bukkit.getPlayer(plugin.debugPlayer);
    }

    /**
     * Log a simple debug message.
     */
    public static void debug(String message) {
        if (!isDebugEnabled()) return;
        send(message);
    }

    /**
     * Log a debug message with a category tag.
     */
    public static void debug(String category, String message) {
        if (!isDebugEnabled()) return;
        send("[" + category + "] " + message);
    }

    /**
     * Log a debug message with a category tag and an associated throwable stack trace.
     */
    public static void debug(String category, String message, Throwable throwable) {
        if (!isDebugEnabled()) return;
        send("[" + category + "] " + message);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }

    /**
     * Routes the message to the appropriate recipient:
     * - If a player enabled debug via command -> send to that player
     * - If debug is enabled via config -> send to console
     * - If both -> player gets it (config debug still goes to console independently via isConfigDebug)
     */
    private static void send(String message) {
        XSetWarps plugin = XSetWarps.getInstance();
        if (plugin == null) return;

        String formatted = ColorUtils.translateColors(PREFIX + message);

        // Player debug via command
        if (plugin.debugPlayer != null) {
            Player debugPlayer = Bukkit.getPlayer(plugin.debugPlayer);
            if (debugPlayer != null && debugPlayer.isOnline()) {
                debugPlayer.sendMessage(formatted);
                return;
            } else {
                // Player went offline, clean up
                plugin.debugPlayer = null;
            }
        }

        // Config debug -> console only
        if (isConfigDebug()) {
            Bukkit.getConsoleSender().sendMessage(formatted);
        }
    }
}