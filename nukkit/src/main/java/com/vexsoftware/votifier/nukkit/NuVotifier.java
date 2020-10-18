package com.vexsoftware.votifier.nukkit;

import com.google.inject.Inject;
import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.support.forwarding.ForwardedVoteListener;
import com.vexsoftware.votifier.support.forwarding.ForwardingVoteSink;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierServerBootstrap;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAIO;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAKeygen;
import com.vexsoftware.votifier.nukkit.cmd.NVReloadCmd;
import com.vexsoftware.votifier.nukkit.cmd.TestVoteCmd;
import com.vexsoftware.votifier.nukkit.event.VotifierEvent;
//import com.vexsoftware.votifier.nukkit.forwarding.NukkitPluginMessagingForwardingSink;
import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.platform.VotifierPlugin;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.util.IOUtil;
import com.vexsoftware.votifier.util.KeyCreator;
import com.vexsoftware.votifier.util.TokenUtil;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.Key;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;

/**
 * @author good777LUCKY
 */
public class NuVotifier extends PluginBase implements VoteHandler, VotifierPlugin, ForwardedVoteListener {

    @Inject
    public Logger logger;
    
    private SLF4JLogger loggerAdapter;
    
    /**
     * The server bootstrap.
     */
    private VotifierServerBootstrap bootstrap;

    /**
     * The RSA key pair.
     */
    private KeyPair keyPair;

    /**
     * Debug mode flag
     */
    private boolean debug;

    /**
     * Keys used for websites.
     */
    private Map<String, Key> tokens = new HashMap<>();

    private ForwardingVoteSink forwardingMethod;
    private VotifierScheduler scheduler;
    
    private boolean loadAndBind() {
        if (!getDataFolder().exists()) {
            if (!getDataFolder().mkdir()) {
                throw new RuntimeException("Unable to create the plugin data folder " + getDataFolder());
            }
        }

        // Handle configuration.
        File config = new File(getDataFolder(), "config.yml");

        /*
         * Use IP address from server.properties as a default for
         * configurations. Do not use InetAddress.getLocalHost() as it most
         * likely will return the main server address instead of the address
         * assigned to the server.
         */
        String hostAddr = getServer().getIp();
        if (hostAddr == null || hostAddr.length() == 0)
            hostAddr = "0.0.0.0";

        /*
         * Create configuration file if it does not exists; otherwise, load it
         */
        if (!config.exists()) {
            try {
                // First time run - do some initialization.
                getLogger().info("Configuring Votifier for the first time...");

                // Initialize the configuration file.
                if (!config.createNewFile()) {
                    throw new IOException("Unable to create the config file at " + config);
                }

                // Load and manually replace variables in the configuration.
                String cfgStr = new String(IOUtil.readAllBytes(getResource("nukkitConfig.yml")), StandardCharsets.UTF_8);
                String token = TokenUtil.newToken();
                cfgStr = cfgStr.replace("%default_token%", token).replace("%ip%", hostAddr);
                Files.copy(new ByteArrayInputStream(cfgStr.getBytes(StandardCharsets.UTF_8)), config.toPath(), StandardCopyOption.REPLACE_EXISTING);

                /*
                 * Remind hosted server admins to be sure they have the right
                 * port number.
                 */
                getLogger().info("------------------------------------------------------------------------------");
                getLogger().info("Assigning NuVotifier to listen on port 8192. If you are hosting NukkitX on a");
                getLogger().info("shared server please check with your hosting provider to verify that this port");
                getLogger().info("is available for your use. Chances are that your hosting provider will assign");
                getLogger().info("a different port, which you need to specify in config.yml");
                getLogger().info("------------------------------------------------------------------------------");
                getLogger().info("Your default NuVotifier token is " + token + ".");
                getLogger().info("You will need to provide this token when you submit your server to a voting");
                getLogger().info("list.");
                getLogger().info("------------------------------------------------------------------------------");
            } catch (Exception ex) {
                getLogger().error("Error creating configuration file", ex);
                return false;
            }
        }

        Config cfg;
        File rsaDirectory = new File(getDataFolder(), "rsa");

        // Load configuration.
        cfg = new Config(config, Config.YAML);

        /*
         * Create RSA directory and keys if it does not exist; otherwise, read
         * keys.
         */
        try {
            if (!rsaDirectory.exists()) {
                if (!rsaDirectory.mkdir()) {
                    throw new RuntimeException("Unable to create the RSA key folder " + rsaDirectory);
                }
                keyPair = RSAKeygen.generate(2048);
                RSAIO.save(rsaDirectory, keyPair);
            } else {
                keyPair = RSAIO.load(rsaDirectory);
            }
        } catch (Exception ex) {
            getLogger().error("Error reading configuration file or RSA tokens", ex);
            return false;
        }

        // the quiet flag always runs priority to the debug flag
        if (cfg.isBoolean("quiet")) {
            debug = !cfg.getBoolean("quiet");
        } else {
            // otherwise, default to being noisy
            debug = cfg.getBoolean("debug", true);
        }

        // Load Votifier tokens.
        ConfigSection tokenSection = cfg.getSection("tokens");

        if (tokenSection != null) {
            Map<String, Object> websites = tokenSection.getAllMap();
            for (Map.Entry<String, Object> website : websites.entrySet()) {
                tokens.put(website.getKey(), KeyCreator.createKeyFrom(website.getValue().toString()));
                getLogger().info("Loaded token for website: " + website.getKey());
            }
        } else {
            String token = TokenUtil.newToken();
            tokenSection = cfg.getSection("tokens");
            tokenSection.set("default", token);
            tokens.put("default", KeyCreator.createKeyFrom(token));
            try {
                cfg.save(config);
            } catch (IOException e) {
                getLogger().error("Error generating Votifier token", e);
                return false;
            }
            getLogger().info("------------------------------------------------------------------------------");
            getLogger().info("No tokens were found in your configuration, so we've generated one for you.");
            getLogger().info("Your default Votifier token is " + token + ".");
            getLogger().info("You will need to provide this token when you submit your server to a voting");
            getLogger().info("list.");
            getLogger().info("------------------------------------------------------------------------------");
        }

        // Initialize the receiver.
        final String host = cfg.getString("host", hostAddr);
        final int port = cfg.getInt("port", 8192);
        if (!debug)
            getLogger().info("QUIET mode enabled!");

        if (port >= 0) {
            final boolean disablev1 = cfg.getBoolean("disable-v1-protocol");
            if (disablev1) {
                getLogger().info("------------------------------------------------------------------------------");
                getLogger().info("Votifier protocol v1 parsing has been disabled. Most voting websites do not");
                getLogger().info("currently support the modern Votifier protocol in NuVotifier.");
                getLogger().info("------------------------------------------------------------------------------");
            }

            this.bootstrap = new VotifierServerBootstrap(host, port, this, disablev1);
            this.bootstrap.start(error -> {});
        } else {
            getLogger().info("------------------------------------------------------------------------------");
            getLogger().info("Your Votifier port is less than 0, so we assume you do NOT want to start the");
            getLogger().info("votifier port server! Votifier will not listen for votes over any port, and");
            getLogger().info("will only listen for pluginMessaging forwarded votes!");
            getLogger().info("------------------------------------------------------------------------------");
        }

        ConfigSection forwardingConfig = cfg.getSection("forwarding");
        if (forwardingConfig != null) {
            String method = forwardingConfig.getString("method", "none").toLowerCase(); //Default to lower case for case-insensitive searches
            if ("none".equals(method)) {
                getLogger().info("Method none selected for vote forwarding: Votes will not be received from a forwarder.");
            } else if ("pluginmessaging".equals(method)) {
                /*String channel = forwardingConfig.getString("pluginMessaging.channel", "NuVotifier");
                try {
                    forwardingMethod = new NukkitPluginMessagingForwardingSink(this, channel, this);
                    getLogger().info("Receiving votes over PluginMessaging channel '" + channel + "'.");
                } catch (RuntimeException e) {
                    getLogger().error("NuVotifier could not set up PluginMessaging for vote forwarding!", e);
                }*/
                getLogger().error("Sorry, NukkitX NuVotifier does not support PluginMessaging for vote forwarding!");
            } else {
                getLogger().error("No vote forwarding method '" + method + "' known. Defaulting to noop implementation.");
            }
        }
        return true;
    }

    private void halt() {
        // Shut down the network handlers.
        if (bootstrap != null) {
            bootstrap.shutdown();
            bootstrap = null;
        }

        if (forwardingMethod != null) {
            forwardingMethod.halt();
            forwardingMethod = null;
        }
    }

    @Override
    public void onEnable() {
        this.scheduler = new NukkitScheduler(this);
        this.loggerAdapter = new SLF4JLogger(logger);
        
        getServer().getCommandMap().register("nvreload", new NVReloadCmd(this));
        getServer().getCommandMap().register("testvote", new TestVoteCmd(this));

        if (!loadAndBind()) {
            gracefulExit();
            setEnabled(false); // safer to just bomb out
        }
    }

    @Override
    public void onDisable() {
        halt();
        getLogger().info("Votifier disabled.");
    }

    public boolean reload() {
        try {
            halt();
        } catch (Exception ex) {
            getLogger().error("On halt, an exception was thrown. This may be fine!", ex);
        }

        if (loadAndBind()) {
            getLogger().info("Reload was successful.");
            return true;
        } else {
            try {
                halt();
                getLogger().error("On reload, there was a problem with the configuration. Votifier currently does nothing!");
            } catch (Exception ex) {
                getLogger().error("On reload, there was a problem loading, and we could not re-halt the server. Votifier is in an unstable state!", ex);
            }
            return false;
        }
    }

    private void gracefulExit() {
        getLogger().error("Votifier did not initialize properly!");
    }

    @Override
    public LoggingAdapter getPluginLogger() {
        return loggerAdapter;
    }

    public Logger getLogger() {
        return logger;
    }
    
    @Override
    public VotifierScheduler getScheduler() {
        return scheduler;
    }

    public boolean isDebug() {
        return debug;
    }

    @Override
    public Map<String, Key> getTokens() {
        return tokens;
    }

    @Override
    public KeyPair getProtocolV1Key() {
        return keyPair;
    }

    @Override
    public void onVoteReceived(final Vote vote, VotifierSession.ProtocolVersion protocolVersion, String remoteAddress) {
        if (debug) {
            getLogger().info("Got a " + protocolVersion.humanReadable + " vote record from " + remoteAddress + " -> " + vote);
        }
        getServer().getScheduler().scheduleTask(this, () -> fireVotifierEvent(vote));
    }

    @Override
    public void onError(Throwable throwable, boolean alreadyHandledVote, String remoteAddress) {
        if (debug) {
            if (alreadyHandledVote) {
                getLogger().error("Vote processed, however an exception occurred with a vote from " + remoteAddress, throwable);
            } else {
                getLogger().error("Unable to process vote from " + remoteAddress, throwable);
            }
        } else if (!alreadyHandledVote) {
            getLogger().error("Unable to process vote from " + remoteAddress);
        }
    }

    @Override
    public void onForward(final Vote v) {
        if (debug) {
            getLogger().info("Got a forwarded vote -> " + v);
        }
        getServer().getScheduler().scheduleTask(this, () -> fireVotifierEvent(v));
    }

    private void fireVotifierEvent(Vote vote) {
        if (VotifierEvent.getHandlerList().getRegisteredListeners().length == 0) {
            getLogger().error("A vote was received, but you don't have any listeners available to listen for it.");
            getLogger().error("See https://github.com/NuVotifier/NuVotifier/wiki/Setup-Guide#vote-listeners for");
            getLogger().error("a list of listeners you can configure.");
        }
        getServer().getPluginManager().callEvent(new VotifierEvent(vote));
    }
}
