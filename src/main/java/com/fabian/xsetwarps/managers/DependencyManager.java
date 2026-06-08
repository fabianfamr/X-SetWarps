package com.fabian.xsetwarps.managers;

import com.fabian.xsetwarps.XSetWarps;
import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.Library;



public class DependencyManager {
    private final XSetWarps plugin;
    private final BukkitLibraryManager libraryManager;

    public DependencyManager(XSetWarps plugin) {
        this.plugin = plugin;
        this.libraryManager = new BukkitLibraryManager(plugin);
    }

    public void loadDependencies() {
        plugin.getLogger().info("Loading dependencies with Libby...");
        libraryManager.addMavenCentral();
        libraryManager.addJitPack(); // Important for XSeries

        // Define Adventure and MiniMessage libraries
        Library minimessage = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-text-minimessage")
                .version("4.14.0")
                .build();

        Library legacy = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-text-serializer-legacy")
                .version("4.14.0")
                .build();
                
        Library api = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-api")
                .version("4.14.0")
                .build();

        Library key = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-key")
                .version("4.14.0")
                .build();
                
        Library gson = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-text-serializer-gson")
                .version("4.14.0")
                .build();

        Library xseries = Library.builder()
                .groupId("com.github.cryptomorin")
                .artifactId("XSeries")
                .version("13.6.0")
                .build();

        // Load libraries
        try {
            plugin.getLogger().info("Downloading Adventure API...");
            libraryManager.loadLibrary(api);
            libraryManager.loadLibrary(key);
            libraryManager.loadLibrary(gson);
            libraryManager.loadLibrary(minimessage);
            libraryManager.loadLibrary(legacy);
            
            plugin.getLogger().info("Downloading XSeries...");
            libraryManager.loadLibrary(xseries);
            
            plugin.getLogger().info("All dependencies loaded successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load dependencies: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
