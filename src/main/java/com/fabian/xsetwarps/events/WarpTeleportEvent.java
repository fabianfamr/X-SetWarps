package com.fabian.xsetwarps.events;

import com.fabian.xsetwarps.model.Warp;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Called just before a player is teleported to a warp.
 * Other plugins can listen to this event and cancel the teleport if needed.
 */
public class WarpTeleportEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;
    private final Warp warp;

    public WarpTeleportEvent(Player player, Warp warp) {
        super(player);
        this.warp = warp;
        this.cancelled = false;
    }

    /** The warp the player is about to be teleported to. */
    public Warp getWarp() {
        return warp;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}