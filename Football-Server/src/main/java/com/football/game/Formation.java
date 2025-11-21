package com.football.game;

import com.football.util.math.geometry.Translation2d;

import java.util.Arrays;

public class Formation {
    public static Formation FOUR_THREE_THREE = new Formation(
            // DEFENDERS
            new Translation2d[]{
                    new Translation2d(0.25, 0.75),
                    new Translation2d(0.25, 0.25),
                    new Translation2d(0.25, -0.25),
                    new Translation2d(0.25, -0.75),
            },
            // MIDFIELDERS
            new Translation2d[]{
                    new Translation2d(0.55, 0.5),
                    new Translation2d(0.55, 0),
                    new Translation2d(0.55, -0.5),
            },
            // ATTACKERS
            new Translation2d[]{
                    new Translation2d(0.82, 0.6),
                    new Translation2d(0.9, 0),
                    new Translation2d(0.82, -0.6)
            }
    );

    private final Translation2d[] attackers;
    private final Translation2d[] midfielders;
    private final Translation2d[] defenders;
    private final Translation2d goalkeeper;

    public Formation(Translation2d[] defenders, Translation2d[] midfielders, Translation2d[] attackers) {
        if (attackers.length + midfielders.length + defenders.length != 10) {
            throw new IllegalArgumentException("Got more / less then 10 positions");
        }

        this.defenders = Arrays.stream(defenders).map(Formation::convertToField).toList().toArray(new Translation2d[0]);
        this.midfielders = Arrays.stream(midfielders).map(Formation::convertToField).toList().toArray(new Translation2d[0]);
        this.attackers = Arrays.stream(attackers).map(Formation::convertToField).toList().toArray(new Translation2d[0]);
        this.goalkeeper = convertToField(new Translation2d(0.02, 0));
    }

    public Translation2d[] getAttackers() {
        return attackers;
    }

    public Translation2d[] getMidfielders() {
        return midfielders;
    }

    public Translation2d[] getDefenders() {
        return defenders;
    }

    public Translation2d getGoalkeeper() {
        return goalkeeper;
    }

    private static Translation2d convertToField(Translation2d position) {
        return new Translation2d(1 - position.getX(), position.getY()).times(Game.MAX_X, Game.MAX_Y).unaryMinus();
    }
}
