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
        try {
            plugin.getLogger().info("Loading runtime dependencies via X-API...");
            loadXSeriesDependency();
            plugin.getLogger().info("All dependencies loaded successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load dependencies: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadXSeriesDependency() {
        Library xseries = Library.builder()
                .groupId("com.github.cryptomorin")
                .artifactId("XSeries")
                .version("13.6.0")
                .build();

        libraryManager.loadLibrary(xseries);
    }
}
