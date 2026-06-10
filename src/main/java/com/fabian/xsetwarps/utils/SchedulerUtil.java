package com.fabian.xsetwarps.utils;

import com.fabian.xsetwarps.XSetWarps;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;

public class SchedulerUtil {

    private static final boolean IS_FOLIA = isClass("io.papermc.paper.threadedregions.RegionizedServer");

    static {
        DebugLogger.debug("SchedulerUtil", "Folia detected: " + IS_FOLIA);
    }

    public static void runTask(XSetWarps plugin, Runnable runnable) {
        if (IS_FOLIA) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method runMethod = scheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class);
                runMethod.invoke(scheduler, plugin, (java.util.function.Consumer<Object>) task -> runnable.run());
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, runnable);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static void runTaskAsync(XSetWarps plugin, Runnable runnable) {
        DebugLogger.debug("SchedulerUtil", "runTaskAsync() called");
        if (IS_FOLIA) {
            try {
                Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Method runMethod = scheduler.getClass().getMethod("runNow", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class);
                runMethod.invoke(scheduler, plugin, (java.util.function.Consumer<Object>) task -> runnable.run());
            } catch (Exception e) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    public static void runLater(XSetWarps plugin, Runnable runnable, long ticks) {
        if (IS_FOLIA) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method runMethod = scheduler.getClass().getMethod("runDelayed", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, long.class);
                runMethod.invoke(scheduler, plugin, (java.util.function.Consumer<Object>) task -> runnable.run(), ticks);
            } catch (Exception e) {
                Bukkit.getScheduler().runTaskLater(plugin, runnable, ticks);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, ticks);
        }
    }

    public static void runAtEntity(XSetWarps plugin, Entity entity, Runnable runnable) {
        if (IS_FOLIA) {
            try {
                Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                Method runMethod = scheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, Runnable.class);
                runMethod.invoke(scheduler, plugin, (java.util.function.Consumer<Object>) task -> runnable.run(), null);
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, runnable);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static void runAtEntityLater(XSetWarps plugin, Entity entity, Runnable runnable, long ticks) {
        if (IS_FOLIA) {
            try {
                Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                Method runMethod = scheduler.getClass().getMethod("runDelayed", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, Runnable.class, long.class);
                runMethod.invoke(scheduler, plugin, (java.util.function.Consumer<Object>) task -> runnable.run(), null, ticks);
            } catch (Exception e) {
                Bukkit.getScheduler().runTaskLater(plugin, runnable, ticks);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, ticks);
        }
    }

    public static TaskWrapper runAtEntityTimer(XSetWarps plugin, Entity entity, Runnable runnable, long delay, long period) {
        if (IS_FOLIA) {
            try {
                Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                Method runMethod = scheduler.getClass().getMethod("runAtFixedRate", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, Runnable.class, long.class, long.class);
                Object scheduledTask = runMethod.invoke(scheduler, plugin, (java.util.function.Consumer<Object>) task -> runnable.run(), null, delay, period);
                return new FoliaTaskWrapper(scheduledTask);
            } catch (Exception e) {
                return new BukkitTaskWrapper(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
            }
        } else {
            return new BukkitTaskWrapper(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
        }
    }

    private static boolean isClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public interface TaskWrapper {
        void cancel();
    }

    private static class BukkitTaskWrapper implements TaskWrapper {
        private final org.bukkit.scheduler.BukkitTask task;
        public BukkitTaskWrapper(org.bukkit.scheduler.BukkitTask task) { this.task = task; }
        @Override public void cancel() { task.cancel(); }
    }

    private static class FoliaTaskWrapper implements TaskWrapper {
        private final Object task;
        public FoliaTaskWrapper(Object task) { this.task = task; }
        @Override public void cancel() {
            try {
                Method cancelMethod = task.getClass().getMethod("cancel");
                cancelMethod.invoke(task);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}