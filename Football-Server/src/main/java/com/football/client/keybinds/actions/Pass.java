package com.football.client.keybinds.actions;

import com.football.client.keybinds.Keybind;
import com.football.client.keybinds.KeybindAction;
import com.football.game.players.Player;
import com.football.game.strategy.TeamStrategy;
import com.football.util.math.geometry.Translation2d;

import java.util.Comparator;
import java.util.Set;

public class Pass implements KeybindAction {
    @Override
    public void justPressed(TeamStrategy teamStrategy, Player player) {}

    @Override
    public void holding(TeamStrategy teamStrategy, Player player) {}

    @Override
    public void justReleased(TeamStrategy teamStrategy, Player player, Set<Keybind> kickTypes, double holdTime) {
        if (!player.hasBall()) return;

        Player playerToPass = getPlayerToPass(teamStrategy, player, holdTime);
        double finalVelocity = computeHoldTime(holdTime, 1.2, 3, 12);

        player.pass(playerToPass, finalVelocity);
        teamStrategy.playerPassedTo(playerToPass);
    }

    protected Player getPlayerToPass(TeamStrategy teamStrategy, Player player, double holdTime) {
        double desiredDist = computeHoldTime(holdTime, 1.6, 4, 50);

        return teamStrategy.getTeam().getPlayers().stream()
                .filter(p -> !p.equals(player))
                .min(Comparator.comparingDouble(p -> {
                    Translation2d delta = p.getPosition().minus(player.getPosition());
                    double dist = delta.getNorm();
                    double angle = Math.abs(delta.getAngle()
                            .minus(teamStrategy.getRequestedVelocity().getAngle())
                            .getRadians());

                    // ---- FIFA-STYLE WEIGHTS ----
                    // Angle is MOST important (cone targeting)
                    double anglePenalty = angle * (holdTime < 0.3 ? 14 : 8);

                    // Distance penalty (pick the distance closest to what the power suggests)
                    double distPenalty = Math.abs(dist - desiredDist) * 0.25;

                    // Backwards passes are discouraged
                    if (angle > Math.PI * 0.8)
                        anglePenalty += 50;

                    return anglePenalty + distPenalty;
                }))
                .orElse(null);
    }
}
