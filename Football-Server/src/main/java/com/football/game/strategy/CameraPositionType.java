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
    }, t -> Rotation2d.kZero),
    THIRD_PERSON(t -> {
        Player player = t.getChosenPlayer();
        Translation2d playerPos = player.getPosition();

        // Player body forward (can be independent of ball)
        Translation2d forward = t.ball.getPosition2d().minus(playerPos).normalized();

        // Target camera position: behind player
        Translation2d offset2d = forward.times(-4.5);
        Translation3d cameraPos = new Translation3d(playerPos.plus(offset2d), 2.2);

        Rotation2d yaw = offset2d.getAngle().plus(Rotation2d.kCCW_Pi_2);
        return new CameraPose(cameraPos, Rotation2d.fromDegrees(-5), yaw, Rotation2d.fromDegrees(45));
    }, t -> t.getCameraManager().getPosition().getYaw()),
    ;

    private final Function<TeamStrategy, CameraPose> cameraPose;
    private final Function<TeamStrategy, Rotation2d> cameraOrientation;

    CameraPositionType(Function<TeamStrategy, CameraPose> cameraPose, Function<TeamStrategy, Rotation2d> cameraOrientation) {
        this.cameraPose = cameraPose;
        this.cameraOrientation = cameraOrientation;
    }

    public CameraPose getCameraPose(TeamStrategy teamStrategy) {
        return this.cameraPose.apply(teamStrategy);
    }

    public Rotation2d getCameraOrientation(TeamStrategy teamStrategy) {
        return this.cameraOrientation.apply(teamStrategy);
    }

    public CameraPositionType getOther() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}
