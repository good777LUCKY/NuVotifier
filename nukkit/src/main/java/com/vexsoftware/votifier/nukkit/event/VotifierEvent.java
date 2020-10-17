package com.vexsoftware.votifier.nukkit.event;

import com.vexsoftware.votifier.model.Vote;

import cn.nukkit.event.Event;
import cn.nukkit.event.HandlerList;

/**
 * @author good777LUCKY
 */
public class VotifierEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    
    private Vote vote;
    
    public VotifierEvent(final Vote vote) {
        this.vote = vote;
    }
    
    
    public Vote getVote() {
        return vote;
    }
    
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
