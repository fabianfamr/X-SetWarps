package com.fabian.xsetwarps.managers;

import com.fabian.xsetwarps.XSetWarps;
import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.Library;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DependencyManager {

    private final XSetWarps plugin;
    private final BukkitLibraryManager libraryManager;

    public DependencyManager(XSetWarps plugin) {
        this.plugin = plugin;
        try {
            Path xapiPath = Paths.get(plugin.getDataFolder().getParent(), "X-API");
            Files.createDirectories(xapiPath);
            this.libraryManager = new BukkitLibraryManager(plugin, xapiPath);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not create X-API directory, using default: " + e.getMessage());
            this.libraryManager = new BukkitLibraryManager(plugin);
        }
        this.libraryManager.addMavenCentral();
        this.libraryManager.addSonatype();
        this.libraryManager.addRepository("https://repo.papermc.io/repository/maven-public/");
        this.libraryManager.addJitPack();
    }

    public void loadDependencies() {
        plugin.getLogger().info("Loading runtime dependencies via X-API...");

        // Load Adventure API (for MiniMessage color support) — non-critical
        try {
            loadAdventureDependencies();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load Adventure API (colors may be limited): " + e.getMessage());
        }

        // Load XSeries (for cross-version materials/sounds) — CRITICAL
        try {
            loadXSeriesDependency();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load XSeries: " + e.getMessage());
            e.printStackTrace();
        }

        plugin.getLogger().info("All dependencies loaded successfully!");
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