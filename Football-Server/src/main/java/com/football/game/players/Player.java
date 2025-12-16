package com.football.game.players;

import com.football.GameManager;
import com.football.game.Ball;
import com.football.game.Game;
import com.football.game.team.Team;
import com.football.game.strategy.TeamStrategy;
import com.football.util.math.MathUtil;
import com.football.util.math.geometry.Rotation2d;
import com.football.util.math.geometry.Translation2d;
import com.football.util.math.geometry.Translation3d;

public abstract class Player {

    public static final double PLAYER_RADIUS = 0.5;
    public static final double MAX_ACCELERATION = 8;
    public static final double MAX_DECELERATION = 10;
    public static final double SPRINT_VELOCITY = 10;
    public static final double WALK_VELOCITY = 4;
    public static final double MAX_SKID_ACCELERATION = 10;
    public static final double CARRYING_BALL_MAX_VELOCITY = 7;

    protected transient final Ball ball;

    protected Translation2d position;
    protected Rotation2d direction;

    protected Translation2d velocity;
    protected transient Translation2d targetVelocity;

    protected transient final Translation2d initialPosition;
    protected transient final Translation2d formationPosition;

    protected Player(Team team, Ball ball, Translation2d position) {
        this.ball = ball;

        this.position = position;
        this.initialPosition = position;
        this.formationPosition = new Translation2d(position.getX() * 2 + Game.MAX_X * team.getSideMultiplier(), position.getY());
        this.direction = new Rotation2d();

        this.velocity = new Translation2d();
        this.targetVelocity = new Translation2d();
    }

    public Translation2d getPosition() {
        return this.position;
    }

    public Translation2d getFormationPosition() {
        return formationPosition;
    }

    public Translation2d getVelocity() {
        return this.velocity;
    }

    public Rotation2d getDirection() {
        return direction;
    }

    public boolean hasBall() {
        return this.equals(this.ball.getCarrier());
    }

    public void resetPosition() {
        this.position = this.initialPosition;
        this.velocity = new Translation2d();
        this.targetVelocity = new Translation2d();
        this.direction = new Rotation2d();
    }

    public void setVelocity(Translation2d targetVelocity) {
        setVelocity(targetVelocity, MAX_ACCELERATION, MAX_DECELERATION, MAX_SKID_ACCELERATION);
    }

    public void setVelocity(Translation2d targetVelocity, double maxAcceleration, double maxDeceleration, double maxSkidAcceleration) {
        if (this.hasBall()) {
            targetVelocity = targetVelocity.limitNorm(CARRYING_BALL_MAX_VELOCITY);
        }

        this.targetVelocity = targetVelocity;

        double currentSpeed = this.velocity.getNorm();
        double targetSpeed = this.targetVelocity.getNorm();

        double acceleration = (targetSpeed - currentSpeed) / GameManager.PERIOD;
        double maxAccel = maxAcceleration * (1 - (this.velocity.getNorm() / SPRINT_VELOCITY));

        // Clamp the rate of change
        double accel = MathUtil.clamp(acceleration, -maxDeceleration, maxAccel);

        double newSpeed = currentSpeed + (accel * GameManager.PERIOD);
        newSpeed = Math.max(newSpeed, 0);
        if (targetSpeed > 0) {
            newSpeed = Math.min(newSpeed, targetSpeed);
        }

        // Keep direction consistent with either target or current velocity
        Rotation2d direction = (targetSpeed > 0)
                ? targetVelocity.getAngle()
                : (currentSpeed > 0 ? this.velocity.getAngle() : this.direction);

        Translation2d newVelocity = new Translation2d(newSpeed, direction);

        Translation2d deltaSpeed = newVelocity.minus(this.velocity);
        deltaSpeed = deltaSpeed.limitNorm(maxSkidAcceleration * GameManager.PERIOD);

        this.velocity = this.velocity.plus(deltaSpeed);

        if (this.velocity.getNorm() > 0) {
            this.direction = this.velocity.getAngle();
        }
    }

    public Rotation2d getWantedDirection() {
        return this.targetVelocity.getAngle();
    }

    public Translation2d getVelocityToPosition(Translation2d targetPosition, double speedPercent) {
        Translation2d delta = targetPosition.minus(this.position);
        if (delta.getNorm() < 5) {
            speedPercent = Math.min(speedPercent, delta.getNorm() / 5);
        }
        return delta.normalized().times(SPRINT_VELOCITY * speedPercent);
    }

    public void moveTowards(Translation2d targetPosition, double speedPercent) {
        setVelocity(getVelocityToPosition(targetPosition, speedPercent));
    }

    public void pass(Player targetPlayer, double finalVelocity) {
        Translation2d target = targetPlayer.getPosition()
                .plus(targetPlayer.getVelocity().times(0.5));

        this.ball.kick(target, finalVelocity, 0.03);
    }

    public void through(Player targetPlayer, double finalVelocity) {
        double[] times = MathUtil.quadraticSolver(-0.5 * Ball.FRICTION_ACCEL, finalVelocity, -this.position.getDistance(targetPlayer.getPosition()));
        double time = times.length == 1 ? times[0] : (times[0] > 0 ? times[0] : times[1]);

        Translation2d futurePos = targetPlayer.getPosition().plus(targetPlayer.getVelocity().times(time));
        this.ball.kick(futurePos, finalVelocity, 0.06);
    }

    public void cross(Player targetPlayer, double finalVelocity) {
        // Crosses are lofted + predictive
        Translation2d target = targetPlayer.getPosition()
                .plus(targetPlayer.getVelocity().times(0.5));

        this.ball.kick(target, finalVelocity, 1);
    }

    public void shoot(Translation3d target, double finalVelocity) {
        this.ball.kick(target, finalVelocity, 1);
    }

    public void shoot(Translation3d velocity) {
        this.ball.kick(velocity);
    }

    public double getTargetScore(Player player, Translation2d target, TeamStrategy strategy) {
        return 0;
    };

    public void update(Team team) {
        this.position = this.position.plus(this.velocity.times(GameManager.PERIOD));

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
