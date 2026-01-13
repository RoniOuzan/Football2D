package com.football.client.keybinds;

import com.football.client.keybinds.actions.*;
import com.football.game.players.Player;
import com.football.game.strategy.TeamStrategy;

import java.util.Set;
import java.util.function.BiConsumer;

public enum Keybind implements KeybindAction {
    SWITCH_PLAYER(),
    SPRINT(),
    PASS(new Pass()),
    CROSS(new Cross()),
    THROUGH(new Through()),
    SHOOT(new Shoot()),
    SWITCH_CAMERA((t, p) -> t.getCameraManager().setPositionType(t.getCameraManager().getPositionType().getOther())),

    SKIP_REPLAY(),

    CHIP(true),
    FINESSE(true),
    TRIVELA(true),
    DRIVEN(true),
    ;

    private final KeybindAction action;
    private final boolean isKickType;

    Keybind(KeybindAction action) {
        this.action = action;
        this.isKickType = false;
    }

    Keybind(BiConsumer<TeamStrategy, Player> action) {
        this.action = new NullKeybind() {
            @Override
            public void justPressed(TeamStrategy teamStrategy, Player player) {
                action.accept(teamStrategy, player);
            }
        };
        this.isKickType = false;
    }

    Keybind() {
        this(new NullKeybind());
    }

    Keybind(boolean isKickType) {
        this.action = new NullKeybind();
        this.isKickType = isKickType;
    }

    @Override
    public void justPressed(TeamStrategy teamStrategy, Player player) {
        this.action.justPressed(teamStrategy, player);
    }

    @Override
    public void holding(TeamStrategy teamStrategy, Player player) {
        this.action.holding(teamStrategy, player);
    }

    @Override
    public void justReleased(TeamStrategy teamStrategy, Player player, Set<Keybind> kickTypes, double holdTime) {
        this.action.justReleased(teamStrategy, player, kickTypes, holdTime);
    }

    public boolean isKickType() {
        return this.isKickType;
    }
}
