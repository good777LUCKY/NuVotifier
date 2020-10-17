package com.vexsoftware.votifier.nukkit.cmd;

import com.vexsoftware.votifier.nukkit.NuVotifier;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.utils.TextFormat;

public class NVReloadCmd extends Command {

    private final NuVotifier plugin;
    
    public NVReloadCmd(NuVotifier plugin) {
        super("nvreload", "Reloads the NuVotifier configuration", "/nvreload");
        this.setPermission("nuvotifier.reload");
        this.commandParameters.clear();
        
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String s, String[] args) {
        if (!this.testPermission(sender)) {
            sender.sendMessage(TextFormat.DARK_RED + "You do not have permission to do this!");
            return true;
        }
        
        sender.sendMessage(TextFormat.GRAY + "Reloading NuVotifier...");
        if (plugin.reload()) {
            sender.sendMessage(TextFormat.DARK_GREEN + "NuVotifier has been reloaded!");
        } else {
            sender.sendMessage(TextFormat.DARK_RED + "Looks like there was a problem reloading NuVotifier, check the console!");
        }
        return true;
    }
}
