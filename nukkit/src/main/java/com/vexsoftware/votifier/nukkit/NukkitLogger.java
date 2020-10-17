package com.vexsoftware.votifier.nukkit;

import com.vexsoftware.votifier.platform.LoggingAdapter;

import cn.nukkit.plugin.PluginLogger;

/**
 * @author good777LUCKY
 */
public class NukkitLogger implements LoggingAdapter {

    private PluginLogger l;
    
    public NukkitLogger(PluginLogger l) {
        this.l = l;
    }

    @Override
    public void error(String s) {
        l.error(s);
    }

    @Override
    public void error(String s, Object... o) {
        l.error(s, o);
    }

    @Override
    public void warn(String s) {
        l.warn(s);
    }

    @Override
    public void warn(String s, Object... o) {
        l.warn(s, o);
    }

    @Override
    public void info(String s) {
        l.info(s);
    }

    @Override
    public void info(String s, Object... o) {
        l.info(s, o);
    }
}
