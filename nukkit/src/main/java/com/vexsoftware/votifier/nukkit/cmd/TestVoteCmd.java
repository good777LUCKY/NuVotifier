package com.vexsoftware.votifier.nukkit.cmd;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.nukkit.NuVotifier;
import com.vexsoftware.votifier.util.ArgsToVote;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.utils.TextFormat;

/**
 * @author good777LUCKY
 */
public class TestVoteCmd extends Command {

    private final NuVotifier plugin;
    
    public TestVoteCmd(NuVotifier plugin) {
        super("testvote", "Sends a test vote to the server", "/testvote [username] [serviceName=?] [username=?] [address=?] [localTimestamp=?] [timestamp=?]");
        this.setPermission("nuvotifier.testvote");
        this.commandParameters.clear();
        
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String s, String[] args) {
        if (!this.testPermission(sender)) {
            sender.sendMessage(TextFormat.DARK_RED + "You do not have permission to do this!");
            return true;
        }
        
        Vote v;
        try {
            v = ArgsToVote.parse(args);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(TextFormat.DARK_RED + "Error while parsing arguments to create test vote: " + e.getMessage());
            sender.sendMessage(TextFormat.GRAY + "Usage hint: /testvote [username] [serviceName=?] [username=?] [address=?] [localTimestamp=?] [timestamp=?]");
            return true;
        }
        
        plugin.onVoteReceived(v, VotifierSession.ProtocolVersion.TEST, "localhost.test");
        sender.sendMessage(TextFormat.GREEN + "Test vote executed: " + v.toString());
        
        return true;
    }
}
