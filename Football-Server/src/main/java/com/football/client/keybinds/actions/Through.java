package com.football.client.keybinds.actions;

import com.football.client.keybinds.Keybind;
import com.football.game.strategy.TeamStrategy;
import com.football.game.players.Player;

import java.util.Set;

public class Through extends Pass {

    @Override
    public void justReleased(TeamStrategy teamStrategy, Player player, Set<Keybind> kickTypes, double holdTime) {
        if (!player.hasBall()) return;

        Player playerToPass = getPlayerToPass(teamStrategy, player, holdTime);

        player.through(playerToPass, KeybindActionConstants.THROUGH_DEFAULT_VELOCITY);
        teamStrategy.playerPassedTo(playerToPass);
    }
}
