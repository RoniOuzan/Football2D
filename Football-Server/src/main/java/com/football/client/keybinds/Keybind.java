package com.football.client.keybinds;

import com.football.client.keybinds.actions.*;
import com.football.game.players.Player;
import com.football.game.strategy.TeamStrategy;

import java.util.function.BiConsumer;

public enum Keybind implements KeybindAction {
    SWITCH_PLAYER(),
    SPRINT(),
    PASS(new Pass()),
    CROSS(new Cross()),
    THROUGH(new Through()),
    SHOOT(new Shoot()),
    SWITCH_CAMERA((t, p) -> t.setCameraPosition(t.getCameraPosition().getOther()))
    ;

    private final KeybindAction action;

    Keybind(KeybindAction action) {
        this.action = action;
    }

    Keybind(BiConsumer<TeamStrategy, Player> action) {
        this.action = new KeybindAction() {
            @Override
            public void justPressed(TeamStrategy teamStrategy, Player player) {
                action.accept(teamStrategy, player);
            }
            @Override
            public void holding(TeamStrategy teamStrategy, Player player) {}
            @Override
            public void justReleased(TeamStrategy teamStrategy, Player player, double holdTime) {}
        };
    }

    Keybind() {
        this(new NullKeybind());
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
    public void justReleased(TeamStrategy teamStrategy, Player player, double holdTime) {
        this.action.justReleased(teamStrategy, player, holdTime);
    }
}
