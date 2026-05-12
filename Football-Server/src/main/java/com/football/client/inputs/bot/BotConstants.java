package com.football.client.inputs.bot;

public class BotConstants {
    // ----------------- Actions -----------------
    // Shooting
    public static final double MAX_POWER_SHOOT_DISTANCE = 20;
    public static final double MIN_POWER_SHOOT_PERCENT = 0.3;
    public static final double DRIVEN_SHOT_WEIGHT = 0.8;

    // Passing
    public static final double MAX_POWER_PASSING_DISTANCE = 40;
    public static final double MIN_POWER_PASSING_PERCENT = 0.2;

    // Other
    public static final double DRIBBLE_WEIGHT_THRESHOLD_TO_SPRING = 0.6;

    // ----------------- Weights -----------------
    // Shooting
    public static final double DEFENDER_CLOSE_DISTANCE_THRESHOLD = 3;
    public static final double DEFENDER_CLOSE_PENALTY = 0.3;
    public static final double MAX_SHOOTING_DISTANCE = 30;

    // Passing Logic
    public static final double PASS_BASE_SCORE = 0.5;
    public static final double PASS_ADVANCEMENT_DIVISOR = 40.0;
    public static final double PASS_DISTANCE_PENALTY_DIVISOR = 60.0;
    public static final double PASS_RISK_PENALTY = 0.6;
    public static final double PASSING_LANE_INTERCEPT_RADIUS = 2.0;

    // Dribbling Logic
    public static final double DRIBBLE_SAFE_DISTANCE_THRESHOLD = 10.0;
    public static final double DRIBBLE_PRESSURE_DISTANCE_THRESHOLD = 5.0;
    public static final double DRIBBLE_UTILITY_SAFE = 0.9;
    public static final double DRIBBLE_UTILITY_PRESSURE = 0.6;
    public static final double DRIBBLE_UTILITY_LOCKED_DOWN = 0.2;
}
