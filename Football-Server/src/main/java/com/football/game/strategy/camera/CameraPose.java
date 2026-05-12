package com.football.game.strategy.camera;

import java.util.Objects;

import com.football.util.math.MathUtil;
import com.football.util.math.geometry.Rotation2d;
import com.football.util.math.geometry.Translation3d;
import com.football.util.math.interpolation.Interpolatable;

public class CameraPose implements Interpolatable<CameraPose> {
    private final Translation3d translation;
    private final Rotation2d pitch;
    private final Rotation2d yaw;
    private final Rotation2d fov;

    public CameraPose(Translation3d position, Rotation2d pitch, Rotation2d yaw, Rotation2d fov) {
        this.translation = position;
        this.pitch = pitch;
        this.yaw = yaw;
        this.fov = fov;
    }

    public CameraPose() {
        this(new Translation3d(), new Rotation2d(), new Rotation2d(), new Rotation2d());
    }

    public Translation3d getTranslation() {
        return this.translation;
    }

    public Rotation2d getPitch() {
        return this.pitch;
    }

    public Rotation2d getYaw() {
        return this.yaw;
    }

    public Rotation2d getFov() {
        return this.fov;
    }

    public CameraPose plus(CameraPose cameraPose) {
        return new CameraPose(
                this.translation.plus(cameraPose.translation),
                this.pitch.plus(cameraPose.pitch),
                this.yaw.plus(cameraPose.yaw),
                this.fov
        );
    }

    public CameraPose minus(CameraPose cameraPose) {
        return new CameraPose(
                this.translation.minus(cameraPose.translation),
                this.pitch.minus(cameraPose.pitch),
                this.yaw.minus(cameraPose.yaw),
                this.fov
        );
    }

    public CameraPose limitNorm(double maxTranslationNorm, double maxAngle) {
        return new CameraPose(
                this.translation.limitNorm(maxTranslationNorm),
                new Rotation2d(MathUtil.clamp(this.pitch.getRadians(), -maxAngle, maxAngle)),
                new Rotation2d(MathUtil.clamp(this.yaw.getRadians(), -maxAngle, maxAngle)),
                this.fov
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CameraPose) obj;
        return Objects.equals(this.translation, that.translation) &&
                Objects.equals(this.yaw, that.yaw) &&
                Objects.equals(this.pitch, that.pitch) &&
                Objects.equals(this.fov, that.fov);
    }

    @Override
    public int hashCode() {
        return Objects.hash(translation, yaw, pitch, fov);
    }

    @Override
    public String toString() {
        return "CameraPose[" +
                "position=" + translation + ", " +
                "yaw=" + yaw + ", " +
                "pitch=" + pitch + ']';
    }

    @Override
    public CameraPose interpolate(CameraPose endValue, double t) {
        return new CameraPose(
                this.translation.interpolate(endValue.translation, t),
                this.pitch.interpolate(endValue.pitch, t),
                this.yaw.interpolate(endValue.yaw, t),
                this.fov.interpolate(endValue.fov, t)
        );
    }
}