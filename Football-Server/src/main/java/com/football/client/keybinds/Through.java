package com.football.client.keybinds;

import com.football.game.strategy.TeamStrategy;
import com.football.game.players.Player;

public class Through extends Pass {
    @Override
    public void justPressed(TeamStrategy teamStrategy, Player player) {}

    @Override
    public void holding(TeamStrategy teamStrategy, Player player) {}

    @Override
    public void justReleased(TeamStrategy teamStrategy, Player player, double holdTime) {
        if (!player.hasBall()) return;

        Player playerToPass = getPlayerToPass(player, teamStrategy.getTeam(), holdTime);

        player.through(playerToPass, 8);
        teamStrategy.playerPassedTo(playerToPass);
    }
}
