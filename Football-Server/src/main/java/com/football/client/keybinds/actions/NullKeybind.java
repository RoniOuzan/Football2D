package com.football.client.keybinds.actions;

import com.football.client.keybinds.Keybind;
import com.football.client.keybinds.KeybindAction;
import com.football.game.strategy.TeamStrategy;
import com.football.game.players.Player;

import java.util.Set;

public class NullKeybind implements KeybindAction {
    @Override
    public void justPressed(TeamStrategy teamStrategy, Player player) {}

    @Override
    public void holding(TeamStrategy teamStrategy, Player player) {}

    @Override
    public void justReleased(TeamStrategy teamStrategy, Player player, Set<Keybind> kickTypes, double holdTime) {}
}
