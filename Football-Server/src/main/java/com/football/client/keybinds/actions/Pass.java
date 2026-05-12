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
        double finalVelocity = computeHoldTime(holdTime, KeybindActionConstants.PASS_VELOCITY_HOLD_TIME_FACTOR, KeybindActionConstants.PASS_VELOCITY_MIN_POWER, KeybindActionConstants.PASS_VELOCITY_MAX_POWER);

        player.pass(playerToPass, finalVelocity);
        teamStrategy.playerPassedTo(playerToPass);
    }

    protected Player getPlayerToPass(TeamStrategy teamStrategy, Player player, double holdTime) {
        double desiredDist = computeHoldTime(holdTime, KeybindActionConstants.PASS_DESIRED_DIST_HOLD_TIME_FACTOR, KeybindActionConstants.PASS_DESIRED_DIST_MIN_POWER, KeybindActionConstants.PASS_DESIRED_DIST_MAX_POWER);

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
                    double anglePenalty = angle * (holdTime < KeybindActionConstants.PASS_ANGLE_PENALTY_HOLD_TIME_THRESHOLD ? KeybindActionConstants.PASS_ANGLE_PENALTY_TIGHT_CONE_FACTOR : KeybindActionConstants.PASS_ANGLE_PENALTY_LOOSE_CONE_FACTOR);

                    // Distance penalty (pick the distance closest to what the power suggests)
                    double distPenalty = Math.abs(dist - desiredDist) * KeybindActionConstants.PASS_DISTANCE_PENALTY_FACTOR;

                    // Backwards passes are discouraged
                    if (angle > KeybindActionConstants.PASS_BACKWARDS_ANGLE_THRESHOLD)
                        anglePenalty += KeybindActionConstants.PASS_BACKWARDS_PENALTY;

                    return anglePenalty + distPenalty;
                }))
                .orElse(null);
    }
}
