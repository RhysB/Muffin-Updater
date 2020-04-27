package com.johnymuffin.beta.updater;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MuffinUpdaterCommand implements CommandExecutor {
    private MuffinUpdater plugin;

    public MuffinUpdaterCommand(MuffinUpdater plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        String updateRequester = ((commandSender instanceof Player) ? ((Player)commandSender).getName() : "Console");
        if (commandSender.hasPermission("muffinupdater.update")) {
            plugin.checkUpdateCommand(updateRequester);
        } else {
            commandSender.sendMessage(ChatColor.RED + "Sorry, you don't have permission to manually check for updates");
        }


        return true;
    }
}
