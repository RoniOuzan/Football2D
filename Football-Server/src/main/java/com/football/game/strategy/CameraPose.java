package com.football.game.strategy;

import com.football.util.math.geometry.Rotation2d;
import com.football.util.math.geometry.Translation3d;

import java.util.Objects;

public class CameraPose {
    private final Translation3d translation;
    private final Rotation2d pitch;
    private final Rotation2d yaw;

    public CameraPose(Translation3d position, Rotation2d pitch, Rotation2d yaw) {
        this.translation = position;
        this.pitch = pitch;
        this.yaw = yaw;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CameraPose) obj;
        return Objects.equals(this.translation, that.translation) &&
                Objects.equals(this.yaw, that.yaw) &&
                Objects.equals(this.pitch, that.pitch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(translation, yaw, pitch);
    }

    @Override
    public String toString() {
        return "CameraPose[" +
                "position=" + translation + ", " +
                "yaw=" + yaw + ", " +
                "pitch=" + pitch + ']';
    }
}