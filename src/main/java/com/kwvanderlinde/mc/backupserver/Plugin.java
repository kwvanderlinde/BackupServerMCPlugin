package com.kwvanderlinde.mc.backupserver;

import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.PaperCommandManager;
import com.kwvanderlinde.mc.backupserver.config.Config;
import com.kwvanderlinde.mc.backupserver.config.Parser;
import com.kwvanderlinde.mc.backupserver.http.FileServer;
import fi.iki.elonen.NanoHTTPD;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

// TODO Move all plugin state to a class that can be initialized via a constructor and `.close()`d for cleanup.
public final class Plugin extends JavaPlugin {
	private BukkitCommandManager commandManager;
	private FileServer fileServer;
	private Config configuration;

	protected Config loadConfiguration() {
		saveDefaultConfig();
		FileConfiguration configuration = getConfig();

		try {
			configuration.load(getDataFolder() + "/config.yml");
		}
		catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}

		return new Parser().parse(configuration);
	}

	public Config getConfiguration() {
		return this.configuration;
	}

	@Override
	public void onEnable() {
		this.getLogger().info(String.format("Initializing %s...", this.getName()));

		final File dataFolder = getDataFolder();
		if (!dataFolder.exists()) {
			boolean mkdirResult = dataFolder.mkdir();
			if (!mkdirResult) {
				throw new RuntimeException("Unable to create plugin data directory " + dataFolder.getPath());
			}
		}

		this.configuration = loadConfiguration();

		this.commandManager = new PaperCommandManager(this);
		this.commandManager.enableUnstableAPI("help");
		//this.commandManager.getCommandCompletions().registerCompletion("something", context -> some_state);
		this.commandManager.registerCommand(new BackupCommand(this));

		try {
			this.fileServer = new FileServer(configuration.getServerAddress().getPort(), this);
			this.fileServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true);
		}
		catch (IOException e) {
			this.getLogger().severe("Unable to start HTTP server");
			this.getLogger().severe(e.toString());
			e.printStackTrace();
			this.fileServer = null;
		}

		this.getLogger().info(String.format("%s initialized! Server running at http://%s/", this.getName(), configuration.getServerAddress()));
	}

	@Override
	public void onDisable() {
		if (fileServer != null) {
			this.fileServer.stop();
			this.fileServer = null;
		}

		if (this.commandManager != null) {
			this.commandManager.unregisterCommands();
			this.commandManager = null;
		}

		this.configuration = null;
	}
}
