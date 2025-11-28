package com.football.client.keybinds;

import com.football.game.strategy.TeamStrategy;
import com.football.game.players.Player;

public enum Keybind implements KeybindAction {
    SWITCH_PLAYER(),
    SPRINT(),
    PASS(new Pass()),
    THROUGH(new Through()),
    SHOOT(new Shoot()),
    ;

    private final KeybindAction action;

    Keybind(KeybindAction action) {
        this.action = action;
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
