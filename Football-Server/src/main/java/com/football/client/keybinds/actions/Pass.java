package com.football.client.keybinds.actions;

import com.football.client.keybinds.KeybindAction;
import com.football.game.Team;
import com.football.game.strategy.TeamStrategy;
import com.football.game.players.Player;
import com.football.util.math.geometry.Translation2d;

import java.util.Comparator;

public class Pass implements KeybindAction {
    @Override
    public void justPressed(TeamStrategy teamStrategy, Player player) {}

    @Override
    public void holding(TeamStrategy teamStrategy, Player player) {}

    @Override
    public void justReleased(TeamStrategy teamStrategy, Player player, double holdTime) {
        if (!player.hasBall()) return;

        Player playerToPass = getPlayerToPass(player, teamStrategy.getTeam(), holdTime);
        double finalVelocity = computeHoldTime(holdTime, 1.2, 3, 12);

        player.pass(playerToPass, finalVelocity);
        teamStrategy.playerPassedTo(playerToPass);
    }

    protected Player getPlayerToPass(Player player, Team team, double holdTime) {
        double desiredDist = computeHoldTime(holdTime, 1.6, 4, 40);

        return team.getPlayers().stream()
                .filter(p -> !p.equals(player))
                .min(Comparator.comparingDouble(p -> {
                    Translation2d delta = p.getPosition().minus(player.getPosition());
                    double dist = delta.getNorm();
                    double angle = Math.abs(delta.getAngle()
                            .minus(player.getWantedDirection())
                            .getRadians());

                    // ---- FIFA-STYLE WEIGHTS ----
                    // 1. Angle is MOST important (cone targeting)
                    double anglePenalty = angle * (holdTime < 0.3 ? 14 : 8);

                    // 2. Distance penalty (pick the distance closest to what the power suggests)
                    double distPenalty = Math.abs(dist - desiredDist) * 0.15;

                    // 3. Backwards passes are discouraged
                    if (angle > Math.PI * 0.8)
                        anglePenalty += 50;

                    return anglePenalty + distPenalty;
                }))
                .orElse(null);
    }
}
