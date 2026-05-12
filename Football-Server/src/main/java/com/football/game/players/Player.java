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

    public static final double MAX_SKID_ACCELERATION = 20;
    public static final double MAX_OMEGA = Math.PI * 4;
    public static final double CARRYING_BALL_MAX_VELOCITY = 7;

    protected transient final Team team;
    protected transient final Ball ball;

    @Getter
    protected Translation2d position;
    @Getter
    protected Rotation2d direction;

    @Getter
    protected Translation2d velocity;
    protected transient Translation2d targetVelocity;

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

    public boolean hasBall() {
        return this.equals(this.ball.getCarrier());
    }

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

    public Translation2d getVelocityToPosition(Translation2d targetPosition, double speedPercent) {
        Translation2d delta = targetPosition.minus(this.position);
        if (delta.getNorm() < 5) {
            speedPercent = Math.min(speedPercent, delta.getNorm() / 5);
        }
        return delta.normalized().times(SPRINT_VELOCITY * speedPercent);
    }

    public void moveTowards(Translation2d targetPosition, double speedPercent) {
        setTargetVelocity(getVelocityToPosition(targetPosition, speedPercent));
    }

    public void addBallAction(Runnable ballAction) {
        this.ballAction = ballAction;
    }

    public void pass(Player targetPlayer, double finalVelocity) {
        this.addBallAction(() -> {
            Translation2d target = targetPlayer.getPosition()
                    .plus(targetPlayer.getVelocity().times(0.5));

            this.ball.kick(target, finalVelocity, 0.03);
        });
    }

    public void through(Player targetPlayer, double finalVelocity) {
        this.addBallAction(() -> {
            double[] times = MathUtil.quadraticSolver(-0.5 * Ball.FRICTION_ACCEL, finalVelocity, -this.position.getDistance(targetPlayer.getPosition()));
            double time = times.length == 1 ? times[0] : (times[0] > 0 ? times[0] : times[1]);

            Translation2d futurePos = targetPlayer.getPosition().plus(targetPlayer.getVelocity().times(time));
            this.ball.kick(futurePos, finalVelocity, 0.06);
        });
    }

    public void cross(Player targetPlayer, double finalVelocity) {
        this.addBallAction(() -> {
            Translation2d target = targetPlayer.getPosition()
                    .plus(targetPlayer.getVelocity().times(0.5));

            this.ball.kick(target, finalVelocity, 1);
        });
    }

    public void shoot(Translation3d target, double finalVelocity, double heightScale, Translation3d spin) {
        this.addBallAction(() -> this.ball.kick(target, finalVelocity, heightScale, spin));
    }

    public void shoot(Translation3d velocity, Translation3d spin) {
        this.addBallAction(() -> this.ball.kick(velocity, spin));
    }

    public double getTargetScore(Player player, Translation2d target, TeamStrategy strategy) {
        return 0;
    }

    private boolean isNearBall() {
        return this.position.getDistance(this.ball.getPosition2d()) <= 0.75;
    }

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

    public void updateHasBall(Translation2d wantedVelocity) {
        if (this.isNearBall() && wantedVelocity.getNorm() > 0.5) {
            double velocity = this.velocity.dot(wantedVelocity.normalized());
            velocity = Math.max(velocity, 0);
            velocity = velocity * 0.5 + wantedVelocity.getNorm() * 0.5;

            Translation3d kick = new Translation3d(velocity, wantedVelocity.getAngle(), 0.2);
            this.ball.dribble(kick, new Translation3d(0, velocity, 0));
        }

        double targetVel = wantedVelocity.getNorm();
        Translation2d targetPos = this.ball.getPredictedPosition(0.3).toTranslation2d();

        if (!this.isNearBall()) {
            targetVel = Math.max(targetVel, getVelocityToPosition(targetPos, 1).getNorm());
        }

        Translation2d delta = new Translation2d(targetVel, targetPos.minus(this.position).getAngle());
        this.setTargetVelocity(delta);
    }

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
        double maxOmega = movingBackward ? MAX_OMEGA * 0.3 : MAX_OMEGA; // slower rotation while walking backward
        omega = MathUtil.clamp(omega, -maxOmega, maxOmega);

        this.direction = this.direction.plus(new Rotation2d(omega * GameManager.PERIOD));
    }

    //----------------- Physics ----------------

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
