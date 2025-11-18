package com.football.game.players;

import com.football.Constants;
import com.football.client.ClientInput;
import com.football.game.*;
import com.football.util.math.MathUtil;
import com.football.util.math.geometry.Rotation2d;
import com.football.util.math.geometry.Translation2d;

import java.util.Comparator;

public abstract class Player {

    private static final double PLAYER_RADIUS = 0.75;
    private static final double MAX_ACCELERATION = 8;
    private static final double MAX_DECELERATION = 10;
    private static final double SPRINT_VELOCITY = 10;
    private static final double WALK_VELOCITY = 4;
    private static final double MAX_SKID_ACCELERATION = 10;
    private static final double CARRYING_BALL_VELOCITY_MULTIPLIER = 0.8;

    protected transient final Team team;
    protected transient final Ball ball;

    protected Translation2d position;
    protected Translation2d originalPosition;
    protected Rotation2d direction;

    protected Translation2d velocity;
    protected transient Translation2d targetVelocity;

    protected Player(Team team, Ball ball, Translation2d position) {
        this.team = team;
        this.ball = ball;

        this.position = position;
        this.originalPosition = new Translation2d(position.getX() * 2 + Game.MAX_X * this.team.getSideMultiplier(), position.getY());
        this.direction = new Rotation2d();

        this.velocity = new Translation2d();
        this.targetVelocity = new Translation2d();
    }

    public Translation2d getPosition() {
        return this.position;
    }

    public Translation2d getOriginalPosition() {
        return originalPosition;
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

    public void setVelocity(Translation2d targetVelocity) {
        this.targetVelocity = targetVelocity;

        double currentSpeed = this.velocity.getNorm();
        double targetSpeed = this.targetVelocity.getNorm();

        double acceleration = (targetSpeed - currentSpeed) / Constants.PERIOD;
        double maxAccel = MAX_ACCELERATION * (1 - (this.velocity.getNorm() / SPRINT_VELOCITY));

        // Clamp the rate of change
        double accel = MathUtil.clamp(acceleration, -MAX_DECELERATION, maxAccel);

        double newSpeed = currentSpeed + (accel * Constants.PERIOD);
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
        deltaSpeed = deltaSpeed.limitVelocity(MAX_SKID_ACCELERATION * Constants.PERIOD);

        this.velocity = this.velocity.plus(deltaSpeed);

        if (this.velocity.getNorm() > 0) {
            this.direction = this.velocity.getAngle();
        }
    }

    public void moveTowards(Translation2d target, double speedPercent) {
        Translation2d delta = target.minus(this.position);
        if (delta.getNorm() < 5) {
            speedPercent = Math.min(speedPercent, delta.getNorm() / 5);
        }
        setVelocity(delta.normalized().times(SPRINT_VELOCITY * speedPercent));
    }

    public void handleControlledMovement(ClientInput input) {
        double velocity = input.isHolding("shift") ? SPRINT_VELOCITY : WALK_VELOCITY;
        if (this.hasBall()) {
            velocity *= CARRYING_BALL_VELOCITY_MULTIPLIER;
        }

        this.setVelocity(input.getRequestedVelocity().times(velocity));

        if (this.hasBall()) {
            if (input.isHolding("e")) {
                Player playerToPass = getPlayerToPass();
                pass(playerToPass);
                this.team.setChosenPlayer(playerToPass);
            } else if (input.isHolding("f")) {
                Player playerToPass = getPlayerToPass();
                through(playerToPass);
                this.team.setChosenPlayer(playerToPass);
            } else if (input.isHolding("r")) {
                shoot();
            }
        }
    }

    private void pass(Player player) {
        Translation2d delta = player.getPosition().minus(this.position);
        this.ball.kick(delta.times(1.2));
    }

    private void through(Player player) {
        Translation2d delta = player.getPosition().minus(this.position);
        this.ball.kick(delta.times(1.2).plus(player.getVelocity()));
    }

    private void shoot() {
        if (this.position.getX() * this.team.getSideMultiplier() < 10) {
            this.ball.kick(new Translation2d(40, this.direction));
        }

        Translation2d opponentGoal = this.team.getOpponent().getOwnGoalPosition();

        Translation2d nearPost = new Translation2d(0,Game.GOAL_WIDTH / 2 - 0.5);
        if (Math.abs(opponentGoal.plus(nearPost).minus(this.position).getAngle().minus(this.direction).getRadians()) <
                Math.abs(opponentGoal.minus(nearPost).minus(this.position).getAngle().minus(this.direction).getRadians())) {
            this.ball.kick(opponentGoal.plus(nearPost).minus(this.position).times(2));
        } else {
            this.ball.kick(opponentGoal.minus(nearPost).minus(this.position).times(2));
        }
    }

    private Player getPlayerToPass() {
        return this.team.getPlayers().stream()
                .filter(p -> !p.equals(this))
                .min(Comparator.comparingDouble(p -> {
                    Translation2d delta = p.getPosition().minus(this.position);
                    double angleDiff = Math.abs(delta.getAngle().minus(this.direction).getRadians());

                    return 1 * angleDiff + 0.05 * delta.getNorm();
                }))
                .orElse(null);
    }

    public void update() {
        this.position = this.position.plus(this.velocity.times(Constants.PERIOD));

        if (this.team.getOpponent() == null) return;

        // Collision check with all players
        for (Player p : this.team.getPlayers()) {
            if (p == this) continue;
            resolveCollision(p);
        }
        for (Player p : this.team.getOpponent().getPlayers()) {
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

        double newX = MathUtil.clamp(x, -Game.MAX_X + PLAYER_RADIUS, Game.MAX_X - PLAYER_RADIUS);
        double newY = MathUtil.clamp(y, -Game.MAX_Y + PLAYER_RADIUS, Game.MAX_Y - PLAYER_RADIUS);

        // If clamped, reduce velocity in direction of impact
        if (newX != x) {
            this.velocity = new Translation2d(0, this.velocity.getY());
        }
        if (newY != y) {
            this.velocity = new Translation2d(this.velocity.getX(), 0);
        }
        this.position = new Translation2d(newX, newY);
    }
}
