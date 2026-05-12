package com.football.client.keybinds.actions;

public class KeybindActionConstants {

    // Pass Action
    public static final double PASS_VELOCITY_HOLD_TIME_FACTOR = 1.2;
    public static final double PASS_VELOCITY_MIN_POWER = 3;
    public static final double PASS_VELOCITY_MAX_POWER = 12;

    public static final double PASS_DESIRED_DIST_HOLD_TIME_FACTOR = 1.6;
    public static final double PASS_DESIRED_DIST_MIN_POWER = 4;
    public static final double PASS_DESIRED_DIST_MAX_POWER = 50;

    public static final double PASS_ANGLE_PENALTY_HOLD_TIME_THRESHOLD = 0.3;
    public static final double PASS_ANGLE_PENALTY_TIGHT_CONE_FACTOR = 14;
    public static final double PASS_ANGLE_PENALTY_LOOSE_CONE_FACTOR = 8;
    public static final double PASS_BACKWARDS_ANGLE_THRESHOLD = Math.PI * 0.8;
    public static final double PASS_BACKWARDS_PENALTY = 50;
    public static final double PASS_DISTANCE_PENALTY_FACTOR = 0.25;

    // Cross Action
    public static final double CROSS_VELOCITY_HOLD_TIME_FACTOR = 1.2;
    public static final double CROSS_VELOCITY_MIN_POWER = 4;
    public static final double CROSS_VELOCITY_MAX_POWER = 8;

    // Through Action
    public static final double THROUGH_DEFAULT_VELOCITY = 6;

    // Shoot Action
    public static final double SHOOT_VELOCITY_HOLD_TIME_FACTOR = 1.3;
    public static final double SHOOT_VELOCITY_MIN_POWER = 15;
    public static final double SHOOT_VELOCITY_MAX_POWER = 35;

    public static final double SHOOT_DEFAULT_TARGET_Z = 1.0;
    public static final double SHOOT_DEFAULT_HEIGHT_SCALE = 0.5;

    // Finesse Kick
    public static final double FINESSE_TARGET_Z = 2.2;
    public static final double FINESSE_HEIGHT_SCALE = 0.8;
    public static final double FINESSE_SPIN_Y = 5;
    public static final double FINESSE_SPIN_Z = 80;

    // Trivela Kick
    public static final double TRIVELA_TARGET_Z = 2.2;
    public static final double TRIVELA_HEIGHT_SCALE = 0.8;
    public static final double TRIVELA_SPIN_Y = 5;
    public static final double TRIVELA_SPIN_Z = -80;

    // Chip Kick
    public static final double CHIP_VELOCITY_DIVISOR = 4;
    public static final double CHIP_TARGET_Z = 1.5;
    public static final double CHIP_HEIGHT_SCALE = 1.0;
    public static final double CHIP_SPIN_Y = -10;
    public static final double CHIP_SPIN_Z = 0;

    // Driven Kick
    public static final double DRIVEN_VELOCITY_DIVISOR = 4;
    public static final double DRIVEN_TARGET_Z = 0.5;
    public static final double DRIVEN_HEIGHT_SCALE = 0.1;
    public static final double DRIVEN_SPIN_Y = 20;
    public static final double DRIVEN_SPIN_Z = 0;

    public static final double SHOOT_NEAR_GOAL_THRESHOLD_X = 10; // Distance from own goal line to trigger direct shot

    // Shot Target Calculation
    public static final double SHOOT_REQUESTED_DIRECTION_NORM_THRESHOLD = 1e-3;
    public static final double SHOOT_MANUAL_TARGET_DISTANCE = 40;
    public static final double SHOOT_GOAL_POST_OFFSET = 1; // Offset from goal center to post for target

    public static final double SHOOT_ASSIST_HOLD_TIME_THRESHOLD = 0.7;
    public static final double SHOOT_ASSIST_POWER_EXPONENT = 1.3;
    public static final double SHOOT_ASSIST_MIN_FACTOR = 0.6;
    public static final double SHOOT_ASSIST_RANGE_FACTOR = 0.4;
}