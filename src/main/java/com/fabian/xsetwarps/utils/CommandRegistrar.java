package com.fabian.xsetwarps.utils;

import com.fabian.xsetwarps.XSetWarps;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utility to register commands dynamically into Bukkit's CommandMap.
 * This allows keeping only the main command in plugin.yml while
 * sub-commands are registered at runtime.
 */
public class CommandRegistrar {

    private final XSetWarps plugin;

    public CommandRegistrar(XSetWarps plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers a command dynamically with a CommandExecutor and optional TabCompleter and aliases.
     */
    public void register(String name, CommandExecutor executor, TabCompleter tabCompleter, String... aliases) {
        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            plugin.logWarning("Could not access CommandMap, skipping /" + name);
            return;
        }

        DynamicPluginCommand cmd = new DynamicPluginCommand(
                name,
                "X-SetWarps command",
                "/" + name,
                executor,
                tabCompleter,
                Arrays.asList(aliases)
        );

        // Remove existing registration if any
        Command existing = commandMap.getCommand(name.toLowerCase());
        if (existing != null) {
            existing.unregister(commandMap);
            try {
                java.lang.reflect.Method getKnownCommands = commandMap.getClass().getMethod("getKnownCommands");
                @SuppressWarnings("unchecked")
                java.util.Map<String, Command> knownCommands = (java.util.Map<String, Command>) getKnownCommands.invoke(commandMap);
                knownCommands.remove(name.toLowerCase());
            } catch (Exception ignored) {
                // Fallback: may not fully unregister on all server versions
            }
        }

        commandMap.register(plugin.getName().toLowerCase(), cmd);
        DebugLogger.debug("CommandRegistrar", "Registered /" + name);
    }

    private CommandMap getCommandMap() {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(Bukkit.getServer());
        } catch (Exception e) {
            plugin.logWarning("Could not access CommandMap: " + e.getMessage());
            return null;
        }
    }

    /**
     * A lightweight dynamic command that wraps a CommandExecutor and optional TabCompleter.
     */
    private static class DynamicPluginCommand extends Command {

        private final CommandExecutor executor;
        private final TabCompleter tabCompleter;

        protected DynamicPluginCommand(String name, String description, String usage,
                                        CommandExecutor executor, TabCompleter tabCompleter,
                                        List<String> aliases) {
            super(name, description, usage, aliases);
            this.executor = executor;
            this.tabCompleter = tabCompleter;
            this.setPermissionMessage("\u00a7cYou don't have permission.");
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            return executor.onCommand(sender, this, label, args);
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            if (tabCompleter != null) {
                List<String> completions = tabCompleter.onTabComplete(sender, this, alias, args);
                if (completions != null) return completions;
            }
            return Collections.emptyList();
        }
    }
}