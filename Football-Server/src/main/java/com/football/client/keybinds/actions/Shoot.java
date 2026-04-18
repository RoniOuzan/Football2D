package com.football.client.keybinds.actions;

import com.football.client.keybinds.Keybind;
import com.football.client.keybinds.KeybindAction;
import com.football.game.Game;
import com.football.game.players.Player;
import com.football.game.strategy.TeamStrategy;
import com.football.game.team.Team;
import com.football.util.math.geometry.Rotation2d;
import com.football.util.math.geometry.Translation2d;
import com.football.util.math.geometry.Translation3d;

import java.util.*;

public class Shoot implements KeybindAction {

    @Override
    public void justPressed(TeamStrategy teamStrategy, Player player) {}

    @Override
    public void holding(TeamStrategy teamStrategy, Player player) {}

    @Override
    public void justReleased(TeamStrategy teamStrategy, Player player, Set<Keybind> kickTypes, double holdTime) {
        if (!player.hasBall()) return;

        double finalVelocity = computeHoldTime(holdTime, 1.3, 15, 35);
        Translation2d requestedDirection = teamStrategy.getRequestedVelocity();

        Translation3d spin = new Translation3d();
        double targetZ = 1;
        double heightScale = 0.5;
        if (kickTypes.contains(Keybind.FINESSE)) {
            targetZ = 2.2;
            heightScale = 0.8;
            spin = new Translation3d(0, 5, 80);
        } else if (kickTypes.contains(Keybind.TRIVELA)) {
            targetZ = 2.2;
            heightScale = 0.8;
            spin = new Translation3d(0, 5, -80);
        } else if (kickTypes.contains(Keybind.CHIP)) {
            finalVelocity /= 4;
            targetZ = 1.5;
            heightScale = 1;
            spin = new Translation3d(0, -10, 0);
        } else if (kickTypes.contains(Keybind.DRIVEN)) {
            finalVelocity /= 4;
            targetZ = 0.5;
            heightScale = 0.1;
            spin = new Translation3d(0, 20, 0);
        }

        if (player.getPosition().getX() * teamStrategy.getTeam().getSideMultiplier() < 10) {
            player.shoot(new Translation3d(
                    finalVelocity,
                    requestedDirection.getAngle(),
                    targetZ * heightScale
            ), spin);
            return;
        }

        Translation2d target = computeShotTarget(player, teamStrategy.getTeam(), requestedDirection, holdTime);
        player.shoot(new Translation3d(target, targetZ), finalVelocity, heightScale, spin);
    }

    private Translation2d computeShotTarget(Player player, Team team, Translation2d requestedDirection, double holdTime) {
        // Where the player is aiming (raw) ----
        Rotation2d direction = requestedDirection.getNorm() < 1e-3 ? player.getDirection() : requestedDirection.getAngle();

        // Aim 40 meters forward
        Translation2d manualTarget = player.getPosition().plus(new Translation2d(40, direction));

        // Ideal scoring target (goal center adjusted) ----
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

        // Assist factor based on hold time ----
        // tap = manual, full power = more assist
        double assist = Math.min(holdTime / 0.7, 1.0);
        assist = Math.pow(assist, 1.3);
        assist = 0.6 + assist * 0.4;

        // Interpolate the target ----
        return manualTarget.times(1 - assist)
                .plus(bestGoalSpot.times(assist));
    }
}
