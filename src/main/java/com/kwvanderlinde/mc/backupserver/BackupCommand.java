package com.kwvanderlinde.mc.backupserver;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import org.bukkit.command.CommandSender;

@CommandAlias("BackupServer|bs")
@CommandPermission("bs.admin")
@Description("Root for all BackupServer commands.")
public class BackupCommand extends BaseCommand {
	private final Plugin plugin;

	public BackupCommand(Plugin plugin) {
		this.plugin = plugin;
	}

	@HelpCommand
	@Description("Show detailed help for subcommand")
	@Syntax("[subcommand]")
	// We need this since ACF erroneously uses the name of the CommandHelp parameter by default.
	public void onHelp(CommandSender sender, CommandHelp help) {
		help.showHelp();
	}

	@Subcommand("reload")
	@Description("Reload all plugin configurations")
	public void onReload(CommandSender sender) {
		sender.sendMessage(String.format("Reloading %s ...", this.plugin.getName()));
		this.plugin.onDisable();
		this.plugin.onEnable();
		sender.sendMessage(String.format("Finished reloading %s ...", this.plugin.getName()));
	}
}
