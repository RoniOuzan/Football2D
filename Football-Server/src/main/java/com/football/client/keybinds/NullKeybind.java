package com.football.client.keybinds;

import com.football.game.TeamStrategy;
import com.football.game.players.Player;

public class NullKeybind implements KeybindAction {
    @Override
    public void justPressed(TeamStrategy teamStrategy, Player player) {}

    @Override
    public void holding(TeamStrategy teamStrategy, Player player) {}

    @Override
    public void justReleased(TeamStrategy teamStrategy, Player player, double holdTime) {}
}
