package com.football.client.keybinds.actions;

import com.football.client.keybinds.Keybind;
import com.football.client.keybinds.KeybindAction;
import com.football.game.Game;
import com.football.game.players.Player;
import com.football.game.strategy.TeamStrategy;
import com.football.game.Team;
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

        double finalVelocity = computeHoldTime(holdTime, KeybindActionConstants.SHOOT_VELOCITY_HOLD_TIME_FACTOR, KeybindActionConstants.SHOOT_VELOCITY_MIN_POWER, KeybindActionConstants.SHOOT_VELOCITY_MAX_POWER);
        Translation2d requestedDirection = teamStrategy.getRequestedVelocity();

        Translation3d spin = new Translation3d();
        double targetZ = KeybindActionConstants.SHOOT_DEFAULT_TARGET_Z;
        double heightScale = KeybindActionConstants.SHOOT_DEFAULT_HEIGHT_SCALE;
        if (kickTypes.contains(Keybind.FINESSE)) {
            targetZ = KeybindActionConstants.FINESSE_TARGET_Z;
            heightScale = KeybindActionConstants.FINESSE_HEIGHT_SCALE;
            spin = new Translation3d(0, KeybindActionConstants.FINESSE_SPIN_Y, KeybindActionConstants.FINESSE_SPIN_Z);
        } else if (kickTypes.contains(Keybind.TRIVELA)) {
            targetZ = KeybindActionConstants.TRIVELA_TARGET_Z;
            heightScale = KeybindActionConstants.TRIVELA_HEIGHT_SCALE;
            spin = new Translation3d(0, KeybindActionConstants.TRIVELA_SPIN_Y, KeybindActionConstants.TRIVELA_SPIN_Z);
        } else if (kickTypes.contains(Keybind.CHIP)) {
            finalVelocity /= KeybindActionConstants.CHIP_VELOCITY_DIVISOR;
            targetZ = KeybindActionConstants.CHIP_TARGET_Z;
            heightScale = KeybindActionConstants.CHIP_HEIGHT_SCALE;
            spin = new Translation3d(0, KeybindActionConstants.CHIP_SPIN_Y, KeybindActionConstants.CHIP_SPIN_Z);
        } else if (kickTypes.contains(Keybind.DRIVEN)) {
            finalVelocity /= KeybindActionConstants.DRIVEN_VELOCITY_DIVISOR;
            targetZ = KeybindActionConstants.DRIVEN_TARGET_Z;
            heightScale = KeybindActionConstants.DRIVEN_HEIGHT_SCALE;
            spin = new Translation3d(0, KeybindActionConstants.DRIVEN_SPIN_Y, KeybindActionConstants.DRIVEN_SPIN_Z);
        }

        if (player.getPosition().getX() * teamStrategy.getTeam().getSideMultiplier() < KeybindActionConstants.SHOOT_NEAR_GOAL_THRESHOLD_X) {
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
        Rotation2d direction = requestedDirection.getNorm() < KeybindActionConstants.SHOOT_REQUESTED_DIRECTION_NORM_THRESHOLD ? player.getDirection() : requestedDirection.getAngle();

        // Aim 40 meters forward
        Translation2d manualTarget = player.getPosition().plus(new Translation2d(KeybindActionConstants.SHOOT_MANUAL_TARGET_DISTANCE, direction));

        // Ideal scoring target (goal center adjusted) ----
        Translation2d goalCenter = team.getOpponent().getOwnGoalPosition();

        // Best FIFA-style scoring target: slightly offset from center
        // Picks the post that is closer to the aim direction
        Translation2d leftPost = goalCenter.plus(new Translation2d(0, -Game.GOAL_WIDTH / 2 + KeybindActionConstants.SHOOT_GOAL_POST_OFFSET));
        Translation2d rightPost = goalCenter.plus(new Translation2d(0, Game.GOAL_WIDTH / 2 - KeybindActionConstants.SHOOT_GOAL_POST_OFFSET));

        Translation2d bestGoalSpot =
                (Math.abs(leftPost.minus(player.getPosition()).getAngle().minus(direction).getRadians()) <
                        Math.abs(rightPost.minus(player.getPosition()).getAngle().minus(direction).getRadians()))
                        ? leftPost
                        : rightPost;

        // Assist factor based on hold time ----
        // tap = manual, full power = more assist
        double assist = Math.min(holdTime / KeybindActionConstants.SHOOT_ASSIST_HOLD_TIME_THRESHOLD, 1.0);
        assist = Math.pow(assist, KeybindActionConstants.SHOOT_ASSIST_POWER_EXPONENT);
        assist = KeybindActionConstants.SHOOT_ASSIST_MIN_FACTOR + assist * KeybindActionConstants.SHOOT_ASSIST_RANGE_FACTOR;

        // Interpolate the target ----
        return manualTarget.times(1 - assist)
                .plus(bestGoalSpot.times(assist));
    }
}
