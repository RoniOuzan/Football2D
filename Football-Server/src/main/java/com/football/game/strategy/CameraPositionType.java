package com.football.game.strategy;

import com.football.GameManager;
import com.football.game.players.Player;
import com.football.util.math.MathUtil;
import com.football.util.math.geometry.Rotation2d;
import com.football.util.math.geometry.Translation2d;
import com.football.util.math.geometry.Translation3d;

import java.util.function.Function;

public enum CameraPositionType {
    BROADCAST(t -> {
        double cameraX = t.ball.getPosition().getX() * 0.3;
        double cameraY = MathUtil.clamp(t.ball.getPosition().getY(), -16, 16) * 0.1;
        return new CameraPose(
                new Translation3d(cameraX, -60 + cameraY, 30),
                Rotation2d.fromDegrees(-28 + cameraY),
                Rotation2d.fromDegrees(-cameraX / 2),
                Rotation2d.fromDegrees(37)
        );
    }),
    THIRD_PERSON(t -> {
        Player player = t.getChosenPlayer();
        Translation2d playerPos = player.getPosition();

        // Define camera offset relative to player body
        double smoothSpeed = 15;     // higher = faster camera response

        // Player body forward (can be independent of ball)
        Translation2d forward = t.ball.getPosition2d().minus(playerPos).normalized();

        // Target camera position: behind player
        Translation2d offset2d = forward.times(-4.5);
        Translation3d targetPos = new Translation3d(playerPos.plus(offset2d), 2.2);

        // Camera pitch (slightly looking down)
        Rotation2d targetPitch = Rotation2d.fromDegrees(-5);
        Rotation2d targetYaw = offset2d.getAngle().plus(Rotation2d.kCCW_Pi_2);

        // Smoothly interpolate from previous camera (lerp) if available
        CameraPose previous = t.getCameraManager().getPosition(); // returns last frame camera pose
        Translation3d cameraPos;
        Rotation2d pitch;
        Rotation2d yaw;

        if (previous != null) {
            // Linear interpolation for position
            cameraPos = previous.getTranslation().interpolate(targetPos, Math.min(1.0, smoothSpeed * GameManager.PERIOD));
            // Interpolate pitch and yaw
            pitch = previous.getPitch().interpolate(targetPitch, Math.min(1.0, smoothSpeed * GameManager.PERIOD));
            yaw = previous.getYaw().interpolate(targetYaw, Math.min(1.0, smoothSpeed * GameManager.PERIOD));
        } else {
            cameraPos = targetPos;
            pitch = targetPitch;
            yaw = targetYaw;
        }

        return new CameraPose(cameraPos, pitch, yaw, Rotation2d.fromDegrees(45));
    }),
    ;

    private final Function<TeamStrategy, CameraPose> cameraPose;

    CameraPositionType(Function<TeamStrategy, CameraPose> cameraPose) {
        this.cameraPose = cameraPose;
    }

    public CameraPose getCameraPose(TeamStrategy teamStrategy) {
        return this.cameraPose.apply(teamStrategy);
    }

    public CameraPositionType getOther() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}
