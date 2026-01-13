package com.football.game.strategy;

import com.football.game.Game;
import com.football.util.math.geometry.Translation2d;

public class HeatmapGenerator {

    private static final int AMOUNT_OF_STEPS = 10;
    private static final double STEPS_X = Game.LENGTH / AMOUNT_OF_STEPS;
    private static final double STEPS_Y = Game.WIDTH / AMOUNT_OF_STEPS;

    private final TeamStrategy teamStrategy;
    private final ScoreCalculator scoreCalculator;

    public HeatmapGenerator(TeamStrategy teamStrategy) {
        this.teamStrategy = teamStrategy;
        this.scoreCalculator = new ScoreCalculator(teamStrategy);
    }

    public void update() {
        this.teamStrategy.scores.clear();
        for (double i = -Game.MAX_X + (STEPS_X / 2); i < Game.MAX_X; i += STEPS_X) {
            for (double j = -Game.MAX_Y + (STEPS_Y / 2); j < Game.MAX_Y; j += STEPS_Y) {
                Translation2d pose = new Translation2d(i, j);
                this.teamStrategy.scores.put(pose, this.scoreCalculator.calculateScore(pose));
            }
        }
    }
}
