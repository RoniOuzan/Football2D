package com.football.game.strategy;

import com.football.game.Game;
import com.football.util.math.geometry.Translation2d;

import java.util.Map;

public class HeatmapGenerator {

    private static final int AMOUNT_OF_STEPS = 10;
    private static final double STEPS_X = Game.LENGTH / AMOUNT_OF_STEPS;
    private static final double HALF_STEP_X = STEPS_X / 2.0;
    private static final double STEPS_Y = Game.WIDTH / AMOUNT_OF_STEPS;
    private static final double HALF_STEP_Y = STEPS_Y / 2.0;

    private final ScoreCalculator scoreCalculator;

    public HeatmapGenerator(TeamStrategy teamStrategy) {
        this.scoreCalculator = new ScoreCalculator(teamStrategy);
    }

    public void update(Map<Translation2d, Double> scores) {
        scores.clear();
        for (double i = -Game.MAX_X + HALF_STEP_X; i < Game.MAX_X; i += STEPS_X) {
            for (double j = -Game.MAX_Y + HALF_STEP_Y; j < Game.MAX_Y; j += STEPS_Y) {
                Translation2d pose = new Translation2d(i, j);

                scores.put(pose, this.scoreCalculator.calculateScore(pose));
            }
        }
    }
}
