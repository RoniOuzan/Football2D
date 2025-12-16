package com.football.client.keybinds.actions;

import com.football.game.players.Player;
import com.football.game.strategy.TeamStrategy;

public class Cross extends Pass {
    @Override
    public void justPressed(TeamStrategy teamStrategy, Player player) {}

    @Override
    public void holding(TeamStrategy teamStrategy, Player player) {}

    @Override
    public void justReleased(TeamStrategy teamStrategy, Player player, double holdTime) {
        if (!player.hasBall()) return;

        Player playerToPass = getPlayerToPass(player, teamStrategy.getTeam(), holdTime);

        player.cross(playerToPass, 4);
        teamStrategy.playerPassedTo(playerToPass);
    }
}
