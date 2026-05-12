package com.football.game.players;

import com.football.game.Ball;
import com.football.game.Game;
import com.football.game.Team;
import com.football.game.strategy.TeamStrategy;
import com.football.util.math.MathUtil;
import com.football.util.math.geometry.Translation2d;

/**
 * Specialization of {@link Player} representing a goalkeeper.
 * This class contains the AI logic for the goalkeeper's positioning,
 * shot-stopping, and ball-charging behavior.
 */
public class Goalkeeper extends Player {

    // --- Constants for Goalkeeper AI behavior ---

    /** The prediction time (in seconds) for the ball's future position. */
    private static final double BALL_PREDICTION_TIME = 0.3;

    /** Minimum ball velocity to consider it a shot on goal. */
    private static final double SHOT_VELOCITY_THRESHOLD = 4.0;

    /** Dot product threshold to determine if the ball's trajectory is towards the goal.
     * A value of 0.8 corresponds to an angle of approximately 36.87 degrees. */
    private static final double TOWARD_GOAL_ANGLE_THRESHOLD = 0.8;

    /** Speed multiplier for the goalkeeper's "jump" action (e.g., diving for a save). */
    private static final double JUMP_SPEED_MULTIPLIER = 2.0;

    /** Distance from goal within which the goalkeeper will consider charging the ball. */
    private static final double CHARGE_BALL_DISTANCE_THRESHOLD = 20.0;

    /** Speed multiplier for charging the ball. */
    private static final double CHARGE_BALL_SPEED_MULTIPLIER = 1.0;

    /** Multiplier for calculating the goalkeeper's depth based on ball distance. */
    private static final double DEPTH_BALL_DISTANCE_MULTIPLIER = 0.2;

    /** Minimum depth the goalkeeper will maintain from the goal line. */
    private static final double MIN_GOALKEEPER_DEPTH = 3.0;

    /** Maximum depth the goalkeeper will push forward when their team has the ball. */
    private static final double MAX_DEPTH_TEAM_HAS_BALL = 20.0;

    /** Maximum depth the goalkeeper will push forward when the opponent has the ball. */
    private static final double MAX_DEPTH_OPPONENT_HAS_BALL = 10.0;

    /** Speed multiplier for general goalkeeper positioning. */
    private static final double GENERAL_POSITIONING_SPEED_MULTIPLIER = 0.7;

    public Goalkeeper(Team team, Ball ball, Translation2d position) {
        super(team, ball, position);
    }

    /**
     * Sets the goalkeeper's target velocity to "jump" or dive towards a specific position.
     * This is typically used for shot-stopping.
     *
     * @param position The target {@link Translation2d} to jump to.
     */
    public void jumpTo(Translation2d position) {
        setTargetVelocity(getVelocityToPosition(position, JUMP_SPEED_MULTIPLIER));
    }

    /**
     * Handles the goalkeeper's AI logic for positioning and reacting to the game state.
     * This method is called every game tick to update the goalkeeper's behavior.
     *
     * @param strategy The current {@link TeamStrategy} providing game context.
     */
    public void handleTarget(TeamStrategy strategy) {
        Team team = strategy.getTeam();
        Translation2d goalCenter = team.getOwnGoalPosition();
        Translation2d predictedBall = this.ball.getPredictedPosition(BALL_PREDICTION_TIME).toTranslation2d();

        double ballDistanceToGoal = this.ball.getPosition().getDistance(goalCenter);

        // --- Shot-stopping logic ---
        // If the ball is loose and moving fast enough to be a shot
        if (this.ball.getCarrier() == null && this.ball.getVelocity().getNorm() > SHOT_VELOCITY_THRESHOLD) {
            Translation2d ballVel = this.ball.getVelocity2d().normalized();
            Translation2d dirToGoal = goalCenter.minus(this.ball.getPosition2d()).normalized();

            // Check if the ball's trajectory is generally towards the goal
            if (ballVel.dot(dirToGoal) > TOWARD_GOAL_ANGLE_THRESHOLD) {
                // Calculate time to reach the goal line (goalkeeper's X position)
                // Assuming goalkeeper's X is the goal line for simplicity
                double timeToGoalLine = (this.position.getX() - this.ball.getPosition().getX()) / ballVel.getX();

                // If the ball is moving towards the goal line (positive time)
                if (timeToGoalLine > 0) {
                    // Predict the Y-coordinate where the ball will cross the goal line
                    double impactY = this.ball.getPosition().getY() + ballVel.getY() * timeToGoalLine;

                    // Clamp the impact Y-coordinate to within the goal posts
                    impactY = MathUtil.clamp(impactY,
                            goalCenter.getY() - Game.GOAL_WIDTH / 2,
                            goalCenter.getY() + Game.GOAL_WIDTH / 2);

                    // Dive to intercept the ball at the predicted impact point
                    this.jumpTo(new Translation2d(this.position.getX(), impactY));
                    return;
                }
            }
        }

        // --- Ball-charging logic ---
        // If the ball is close to the goal, opponent has possession, and no other teammate is closer
        if (ballDistanceToGoal < CHARGE_BALL_DISTANCE_THRESHOLD && team.getOpponent().hasBall() &&
                team.getPlayers().stream().noneMatch(p -> p.getPosition().getDistance(this.ball.getPosition2d()) < ballDistanceToGoal)) {
            this.moveTowards(predictedBall, CHARGE_BALL_SPEED_MULTIPLIER); // Charge the ball
            return;
        }

        // If our team doesn't have the ball, the goalkeeper is the designated chaser,
        // and the opponent's closest player is further from the ball than the goalkeeper
        if (!team.getOpponent().hasBall() && strategy.getBallChaser().equals(this) &&
                team.getOpponent().getClosestPlayerToBall().getPosition().getDistance(predictedBall) < ballDistanceToGoal) { // This condition seems a bit off, it implies the opponent's closest player is *further* from the ball than the GK, which would mean the GK should charge.
            this.moveTowards(predictedBall, CHARGE_BALL_SPEED_MULTIPLIER);
            return;
        }

        // --- General positioning logic ---
        // The goalkeeper's depth (how far they are from the goal line)
        // is proportional to the ball's distance from the goal.
        double keeperDepth = MathUtil.clamp(ballDistanceToGoal * DEPTH_BALL_DISTANCE_MULTIPLIER,
                MIN_GOALKEEPER_DEPTH,
                team.hasBall() ? MAX_DEPTH_TEAM_HAS_BALL : MAX_DEPTH_OPPONENT_HAS_BALL);

        // Calculate an aim point along the line from the ball to the goal center,
        // then position the goalkeeper at the calculated depth along this line.
        Translation2d aimPoint = predictedBall.minus(goalCenter).normalized();
        this.moveTowards(goalCenter.plus(aimPoint.times(keeperDepth)), GENERAL_POSITIONING_SPEED_MULTIPLIER);
    }
}
