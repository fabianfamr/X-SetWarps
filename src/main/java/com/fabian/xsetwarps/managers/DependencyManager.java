package com.fabian.xsetwarps.managers;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.utils.DebugLogger;
import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.Library;
import net.byteflux.libby.LibraryManager;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DependencyManager {

    private final XSetWarps plugin;
    private final BukkitLibraryManager libraryManager;

    public DependencyManager(XSetWarps plugin) {
        this.plugin = plugin;
        this.libraryManager = new BukkitLibraryManager(plugin);
        setSharedLibraryPath();
        this.libraryManager.addMavenCentral();
        this.libraryManager.addSonatype();
        this.libraryManager.addRepository("https://repo.papermc.io/repository/maven-public/");
        this.libraryManager.addJitPack();
    }

    /**
     * Redirects libby's save directory to plugins/X-API/ so all plugins
     * share a single dependency folder instead of each having their own libs/.
     */
    private void setSharedLibraryPath() {
        try {
            Path sharedPath = Paths.get(plugin.getDataFolder().getParent(), "X-API");
            Files.createDirectories(sharedPath);
            Field field = LibraryManager.class.getDeclaredField("saveDirectory");
            field.setAccessible(true);
            field.set(libraryManager, sharedPath);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not set shared library path (plugins/X-API/): " + e.getMessage());
        }
    }

    public void loadDependencies() {
        DebugLogger.debug("DependencyManager", "Loading runtime dependencies via X-API...");
        plugin.getLogger().info("Loading runtime dependencies via X-API...");

        // Load Adventure API (for MiniMessage color support) — non-critical
        try {
            DebugLogger.debug("DependencyManager", "Loading Adventure API dependencies...");
            loadAdventureDependencies();
            DebugLogger.debug("DependencyManager", "Adventure API loaded successfully");
        } catch (Exception e) {
            DebugLogger.debug("DependencyManager", "Failed to load Adventure API", e);
            plugin.getLogger().warning("Failed to load Adventure API (colors may be limited): " + e.getMessage());
        }

        // Load XSeries (for cross-version materials/sounds) — CRITICAL
        try {
            DebugLogger.debug("DependencyManager", "Loading XSeries dependency...");
            loadXSeriesDependency();
            DebugLogger.debug("DependencyManager", "XSeries loaded successfully");
        } catch (Exception e) {
            DebugLogger.debug("DependencyManager", "Failed to load XSeries", e);
            plugin.getLogger().severe("Failed to load XSeries: " + e.getMessage());
            e.printStackTrace();
        }

        plugin.getLogger().info("All dependencies loaded successfully!");
        DebugLogger.debug("DependencyManager", "All dependencies loaded successfully");
    }

    private void loadAdventureDependencies() {
        libraryManager.loadLibrary(Library.builder()
                .groupId("net.kyori").artifactId("adventure-api").version("4.14.0").build());
        libraryManager.loadLibrary(Library.builder()
                .groupId("net.kyori").artifactId("adventure-text-minimessage").version("4.14.0").build());
        libraryManager.loadLibrary(Library.builder()
                .groupId("net.kyori").artifactId("adventure-text-serializer-legacy").version("4.14.0").build());
        libraryManager.loadLibrary(Library.builder()
                .groupId("net.kyori").artifactId("adventure-text-serializer-plain").version("4.14.0").build());
        libraryManager.loadLibrary(Library.builder()
                .groupId("net.kyori").artifactId("adventure-key").version("4.14.0").build());
        libraryManager.loadLibrary(Library.builder()
                .groupId("net.kyori").artifactId("examination-api").version("1.3.0").build());
        libraryManager.loadLibrary(Library.builder()
                .groupId("net.kyori").artifactId("examination-string").version("1.3.0").build());
    }

    private void loadXSeriesDependency() {
        libraryManager.loadLibrary(Library.builder()
                .groupId("com.github.cryptomorin").artifactId("XSeries").version("13.6.0").build());
    }
}