package com.football.client.keybinds;

import com.football.client.ClientInput;
import com.football.game.Game;
import com.football.game.Team;
import com.football.game.TeamStrategy;
import com.football.game.players.Player;
import com.football.util.math.MathUtil;
import com.football.util.math.geometry.Rotation2d;
import com.football.util.math.geometry.Translation2d;

import java.util.Comparator;

public class Shoot implements KeybindAction {
    @Override
    public void justPressed(TeamStrategy teamStrategy, Player player) {}

    @Override
    public void holding(TeamStrategy teamStrategy, Player player) {}

    @Override
    public void justReleased(TeamStrategy teamStrategy, Player player, double holdTime) {
        if (!player.hasBall()) return;

        double finalVelocity = computeHoldTime(holdTime, 1.3, 15, 35);
        if (player.getPosition().getX() * teamStrategy.getTeam().getSideMultiplier() < 10) {
            player.shoot(new Translation2d(finalVelocity, player.getWantedDirection()));
            return;
        }

        Translation2d target = computeShotTarget(player, teamStrategy.getTeam(), teamStrategy.getInput(), holdTime);

        player.shoot(target, finalVelocity);
    }

    private Translation2d computeShotTarget(Player player, Team team, ClientInput input, double holdTime) {
        // ---- 1. Where the player is aiming (raw) ----
        Translation2d requestedDirection = input.getRequestedVelocity();
        Rotation2d direction = requestedDirection.getNorm() < 1e-3 ? player.getDirection() : requestedDirection.getAngle();

        // Aim 40 meters forward
        Translation2d manualTarget = player.getPosition().plus(new Translation2d(40, direction));

        // ---- 2. Ideal scoring target (goal center adjusted) ----
        Translation2d goalCenter = team.getOpponent().getOwnGoalPosition();

        // Best FIFA-style scoring target: slightly offset from center
        // Picks the post that is closer to the aim direction
        Translation2d leftPost = goalCenter.plus(new Translation2d(0, -Game.GOAL_WIDTH / 2 + 1));
        Translation2d rightPost = goalCenter.plus(new Translation2d(0, Game.GOAL_WIDTH / 2 - 1));

        Translation2d bestGoalSpot =
                (Math.abs(leftPost.minus(player.getPosition()).getAngle().minus(direction).getRadians()) <
                        Math.abs(rightPost.minus(player.getPosition()).getAngle().minus(direction).getRadians()))
                        ? leftPost
                        : rightPost;

        // ---- 3. Assist factor based on hold time ----
        // tap = manual, full power = more assist
        double assist = Math.min(holdTime / 0.7, 1.0);
        assist = Math.pow(assist, 1.3);   // FIFA-like curve
        assist = MathUtil.clamp(assist, 0.3, 0.9);

        // ---- 4. Interpolate the target ----
        return manualTarget.times(1 - assist)
                .plus(bestGoalSpot.times(assist));
    }
}
