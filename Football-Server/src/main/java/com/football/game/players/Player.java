package com.football.game.players;

import com.football.GameManager;
import com.football.game.Ball;
import com.football.game.Game;
import com.football.game.Team;
import com.football.game.strategy.TeamStrategy;
import com.football.util.math.MathUtil;
import com.football.util.math.geometry.Rotation2d;
import com.football.util.math.geometry.Translation2d;
import com.football.util.math.geometry.Translation3d;
import lombok.Getter;

public abstract class Player {

    public static final double PLAYER_RADIUS = 0.5;
    public static final double PLAYER_HEIGHT = 1.8;

    public static final double MAX_ACCELERATION = 8;
    public static final double MAX_DECELERATION = 15;
    public static final double SPRINT_VELOCITY = 10;
    public static final double WALK_VELOCITY = 4;

    /** Acceleration applied when changing direction sharply (skidding). */
    public static final double MAX_SKID_ACCELERATION = 20;
    /** Maximum angular velocity (rotation speed) in radians per second. */
    public static final double MAX_OMEGA = Math.PI * 4; // 720 deg/s
    /** Speed limit imposed on the player while they are in possession of the ball. */
    public static final double CARRYING_BALL_MAX_VELOCITY = 7;

    private static final double NEAR_BALL_THRESHOLD = 0.75;
    private static final double SLOWDOWN_DISTANCE_THRESHOLD = 5.0;
    private static final double DRIBBLE_KICK_HEIGHT = 0.2;
    private static final double DRIBBLE_PREDICTION_TIME = 0.3;
    private static final double BACKWARD_ROTATION_SPEED_FACTOR = 0.3;

    protected transient final Team team;
    /** Reference to the game ball. */
    protected transient final Ball ball;

    @Getter
    protected Translation2d position;
    @Getter
    protected Rotation2d direction;

    @Getter
    protected Translation2d velocity;
    protected transient Translation2d targetVelocity;
    
    /** A pending action (kick/pass/shoot) to be executed when the player is close enough to the ball. */
    private transient Runnable ballAction = null;

    @Getter
    protected transient Translation2d formationPosition;
    protected transient final Translation2d originalPosition;

    protected Player(Team team, Ball ball, Translation2d position) {
        this.team = team;
        this.ball = ball;

        this.originalPosition = position;
        this.resetPosition();
    }

    /**
     * Checks if this player is currently carrying the ball.
     */
    public boolean hasBall() {
        return this.equals(this.ball.getCarrier());
    }

    /**
     * Resets the player to their starting formation position and clears velocity.
     * Positions are mirrored based on the team's side multiplier.
     */
    public void resetPosition() {
        this.formationPosition = new Translation2d(this.originalPosition.getX() * 2 + Game.MAX_X, this.originalPosition.getY())
            .times(this.team.getSideMultiplier());
        this.position = this.originalPosition.times(this.team.getSideMultiplier());

        this.velocity = new Translation2d();
        this.targetVelocity = new Translation2d();
        this.direction = new Rotation2d();
    }

    public void setTargetVelocity(Translation2d targetVelocity) {
        if (this.hasBall()) {
            targetVelocity = targetVelocity.limitNorm(CARRYING_BALL_MAX_VELOCITY);
        }
        this.targetVelocity = targetVelocity;
    }

    /**
     * Calculates a velocity vector aimed at a target position.
     * Automatically applies a slowdown effect as the player approaches the target.
     * 
     * @param targetPosition The coordinate to move toward.
     * @param speedPercent   A multiplier for the base SPRINT_VELOCITY (0.0 to 1.0).
     */
    public Translation2d getVelocityToPosition(Translation2d targetPosition, double speedPercent) {
        Translation2d delta = targetPosition.minus(this.position);
        if (delta.getNorm() < SLOWDOWN_DISTANCE_THRESHOLD) {
            speedPercent = Math.min(speedPercent, delta.getNorm() / SLOWDOWN_DISTANCE_THRESHOLD);
        }
        return delta.normalized().times(SPRINT_VELOCITY * speedPercent);
    }

    /**
     * Sets the player's target velocity to move toward a specific position.
     */
    public void moveTowards(Translation2d targetPosition, double speedPercent) {
        setTargetVelocity(getVelocityToPosition(targetPosition, speedPercent));
    }

    public void addBallAction(Runnable ballAction) {
        this.ballAction = ballAction;
    }

    /**
     * Schedules a direct pass to a teammate. 
     * Predicts the teammate's position slightly ahead based on their current velocity.
     */
    public void pass(Player targetPlayer, double finalVelocity) {
        this.addBallAction(() -> {
            Translation2d target = targetPlayer.getPosition()
                    .plus(targetPlayer.getVelocity().times(0.5));

            this.ball.kick(target, finalVelocity, 0.03);
        });
    }

    /**
     * Schedules a through-ball pass.
     * Solves for the intersection of the ball's friction-decelerated path and the player's movement.
     */
    public void through(Player targetPlayer, double finalVelocity) {
        this.addBallAction(() -> {
            double[] times = MathUtil.quadraticSolver(-0.5 * Ball.FRICTION_ACCEL, finalVelocity, -this.position.getDistance(targetPlayer.getPosition()));
            double time = times.length == 1 ? times[0] : (times[0] > 0 ? times[0] : times[1]);

            Translation2d futurePos = targetPlayer.getPosition().plus(targetPlayer.getVelocity().times(time));
            this.ball.kick(futurePos, finalVelocity, 0.06);
        });
    }

    /**
     * Schedules a cross. Similar to a pass but with a high vertical height scale.
     */
    public void cross(Player targetPlayer, double finalVelocity) {
        this.addBallAction(() -> {
            Translation2d target = targetPlayer.getPosition()
                    .plus(targetPlayer.getVelocity().times(0.5));

            this.ball.kick(target, finalVelocity, 1);
        });
    }

    /**
     * Schedules a shot with specific target coordinates and spin.
     */
    public void shoot(Translation3d target, double finalVelocity, double heightScale, Translation3d spin) {
        this.addBallAction(() -> this.ball.kick(target, finalVelocity, heightScale, spin));
    }

    /**
     * Schedules a shot with a pre-calculated velocity vector.
     */
    public void shoot(Translation3d velocity, Translation3d spin) {
        this.addBallAction(() -> this.ball.kick(velocity, spin));
    }

    // 0 on default, should be overrided when wanting to add specific role score
    public double getTargetScore(Player player, Translation2d target, TeamStrategy strategy) {
        return 0;
    }

    /**
     * Returns true if the player is within physical range to interact with the ball.
     */
    private boolean isNearBall() {
        return this.position.getDistance(this.ball.getPosition2d()) <= NEAR_BALL_THRESHOLD;
    }

    /**
     * Updates the player's physics state, processes ball actions, 
     * resolves collisions with other players, and enforces field boundaries.
     * Called every game tick.
     */
    public void update(Team team) {
        updateVelocity();
        this.position = this.position.plus(this.velocity.times(GameManager.PERIOD));

        if (this.ballAction != null && this.isNearBall()) {
            this.ballAction.run();
            this.ballAction = null;
        }

        // Collision check with all players
        for (Player p : team.getPlayers()) {
            if (p == this) continue;
            resolveCollision(p);
        }
        for (Player p : team.getOpponent().getPlayers()) {
            resolveCollision(p);
        }

        keepInsideField();
    }

    //----------------- Movement ----------------

    /**
     * Handles the specific movement logic for a player in possession of the ball.
     * Simulates "dribbling" by applying small kicks to the ball in the direction of movement.
     * 
     * @param wantedVelocity The direction the player intends to move.
     */
    public void updateHasBall(Translation2d wantedVelocity) {
        if (this.isNearBall() && wantedVelocity.getNorm() > 0.5) {
            double velocity = this.velocity.dot(wantedVelocity.normalized());
            velocity = Math.max(velocity, 0);
            velocity = (velocity + wantedVelocity.getNorm()) / 2.0;

            Translation3d kick = new Translation3d(velocity, wantedVelocity.getAngle(), DRIBBLE_KICK_HEIGHT);
            this.ball.dribble(kick, new Translation3d(0, velocity, 0));
        }

        double targetVel = wantedVelocity.getNorm();
        Translation2d targetPos = this.ball.getPredictedPosition(DRIBBLE_PREDICTION_TIME).toTranslation2d();

        if (!this.isNearBall()) {
            targetVel = Math.max(targetVel, getVelocityToPosition(targetPos, 1).getNorm());
        }

        Translation2d delta = new Translation2d(targetVel, targetPos.minus(this.position).getAngle());
        this.setTargetVelocity(delta);
    }

    /**
     * Interpolates the current velocity toward the target velocity.
     * Respects acceleration limits and simulates "skidding" when making sharp turns.
     */
    private void updateVelocity() {
        double currentSpeed = this.velocity.getNorm();
        double targetSpeed = this.targetVelocity.getNorm();

        double acceleration = (targetSpeed - currentSpeed) / GameManager.PERIOD;
        double maxAccel = MAX_ACCELERATION * (1 - (this.velocity.getNorm() / SPRINT_VELOCITY));

        // Clamp the rate of change
        double accel = MathUtil.clamp(acceleration, -MAX_DECELERATION, maxAccel);

        Translation2d newVelocity = getTranslation2d(currentSpeed, accel, targetSpeed);

        Translation2d deltaSpeed = newVelocity.minus(this.velocity);
        deltaSpeed = deltaSpeed.limitNorm(MAX_SKID_ACCELERATION * GameManager.PERIOD);

        this.velocity = this.velocity.plus(deltaSpeed);

        if (this.velocity.getNorm() > 0) {
            updateDirection();
        }
    }

    private Translation2d getTranslation2d(double currentSpeed, double accel, double targetSpeed) {
        double newSpeed = currentSpeed + (accel * GameManager.PERIOD);
        newSpeed = Math.max(newSpeed, 0);
        if (targetSpeed > 0) {
            newSpeed = Math.min(newSpeed, targetSpeed);
        }

        // Keep direction consistent with either target or current velocity
        Rotation2d direction = (targetSpeed > 0)
                ? this.targetVelocity.getAngle()
                : (currentSpeed > 0 ? this.velocity.getAngle() : this.direction);

        return new Translation2d(newSpeed, direction);
    }

    /**
     * Rotates the player's direction to match their movement.
     * Allows for slower rotation when the player is moving backward relative to their facing.
     */
    private void updateDirection() {
        // Choose movement direction (target or current velocity)
        Translation2d movementDirection = this.targetVelocity.getNorm() > 0 ? this.targetVelocity : this.velocity;

        if (movementDirection.getNorm() < 0.01) {
            // Not moving
            return;
        }

        // Determine if moving backward relative to current facing
        boolean movingBackward = movementDirection.dot(this.direction.toTranslation()) < 0;

        // Smoothly rotate toward movement direction over time
        // When moving backward, we allow a small rotation toward movement gradually
        double omega = movementDirection.getAngle().minus(this.direction).getRadians() / GameManager.PERIOD;
        double maxOmega = movingBackward ? MAX_OMEGA * BACKWARD_ROTATION_SPEED_FACTOR : MAX_OMEGA; 
        omega = MathUtil.clamp(omega, -maxOmega, maxOmega);

        this.direction = this.direction.plus(new Rotation2d(omega * GameManager.PERIOD));
    }

    //----------------- Physics ----------------

    /**
     * Simple circle-circle collision resolution.
     * Pushes players apart if they overlap and adjusts velocities to prevent 
     * them from passing through one another.
     */
    private void resolveCollision(Player other) {
        double minDist = PLAYER_RADIUS * 2;
        double distance = this.position.getDistance(other.position);

        // No collision
        if (distance >= minDist || distance == 0) return;

        // How much they overlap
        double overlap = minDist - distance;

        Translation2d pushDir = this.position.minus(other.position).normalized();
        Translation2d push = pushDir.times(overlap * 0.5);

        this.position = this.position.plus(push);
        other.position = other.position.minus(push);

        // Reduce velocity along the collision axis (players slide around)
        Translation2d relativeVel = this.velocity.minus(other.velocity);
        double impact = relativeVel.dot(pushDir);

        if (impact > 0) {
            // Remove only the component causing players to push into each other
            Translation2d correction = pushDir.times(impact * 0.5);

            this.velocity = this.velocity.minus(correction);
            other.velocity = other.velocity.plus(correction);
        }
    }

    /**
     * Constraints the player's position to within the pitch boundaries.
     * Account for the extra depth available inside the goal nets.
     */
    private void keepInsideField() {
        double x = this.position.getX();
        double y = this.position.getY();

        double maxX;
        double maxY;
        if (Math.abs(y) <= Game.GOAL_WIDTH / 2 - PLAYER_RADIUS) {
            maxX = Game.MAX_X + Game.GOAL_DEPTH;
            maxY = Game.GOAL_WIDTH / 2;
        } else {
            maxX = Game.MAX_X;
            maxY = Game.MAX_Y;
        }

        double newX = MathUtil.clamp(x, -(maxX - PLAYER_RADIUS), maxX - PLAYER_RADIUS);
        double newY = MathUtil.clamp(y, -(maxY - PLAYER_RADIUS), maxY - PLAYER_RADIUS);

        // If clamped, reduce velocity in direction of impact
        if (newX != x) {
            this.velocity = new Translation2d(-this.velocity.getX(), this.velocity.getY());
        }
        if (newY != y) {
            this.velocity = new Translation2d(this.velocity.getX(), -this.velocity.getY());
        }
        this.position = new Translation2d(newX, newY);
    }
}
