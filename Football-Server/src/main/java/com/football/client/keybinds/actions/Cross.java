package com.football.client.keybinds.actions;

import com.football.client.keybinds.Keybind;
import com.football.game.players.Player;
import com.football.game.strategy.TeamStrategy;

import java.util.Set;

public class Cross extends Pass {
    @Override
    public void justPressed(TeamStrategy teamStrategy, Player player) {}

    @Override
    public void holding(TeamStrategy teamStrategy, Player player) {}

    @Override
    public void justReleased(TeamStrategy teamStrategy, Player player, Set<Keybind> kickTypes, double holdTime) {
        if (!player.hasBall()) return;

        Player playerToPass = getPlayerToPass(teamStrategy, player, holdTime);

        double finalVelocity = computeHoldTime(holdTime, 1.2, 4, 8);

        player.cross(playerToPass, finalVelocity);
        teamStrategy.playerPassedTo(playerToPass);
    }
}
