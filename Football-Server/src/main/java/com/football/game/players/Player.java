package com.football.game.players;

import com.football.Constants;
import com.football.client.ClientInput;
import com.football.game.Ball;
import com.football.game.Element;
import com.football.game.Team;
import com.football.util.math.MathUtil;
import com.football.util.math.geometry.Rotation2d;
import com.football.util.math.geometry.Translation2d;

import java.util.Comparator;

public abstract class Player implements Element {

    private static final double MAX_ACCELERATION = 8;
    private static final double MAX_DECELERATION = 10;
    private static final double SPRINT_VELOCITY = 10;
    private static final double WALK_VELOCITY = 4;
    private static final double MAX_SKID_ACCELERATION = 10;
    private static final double CARRYING_BALL_VELOCITY_MULTIPLIER = 0.8;

    protected transient final Team team;
    protected transient final Ball ball;

    protected Translation2d position;
    protected Rotation2d direction;

    protected Translation2d velocity;
    protected Translation2d targetVelocity;

    protected Player(Team team, Ball ball, Translation2d position) {
        this.team = team;
        this.ball = ball;

        this.position = position;
        this.direction = new Rotation2d();

        this.velocity = new Translation2d();
        this.targetVelocity = new Translation2d();
    }

    public Translation2d getPosition() {
        return this.position;
    }

    public Translation2d getVelocity() {
        return this.velocity;
    }

    public Rotation2d getDirection() {
        return direction;
    }

    public boolean isCarryingTheBall() {
        return this.equals(this.ball.getCarrier());
    }

    protected void setVelocity(Translation2d targetVelocity) {
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

    public abstract void handleMovement();

    public void handleControlledMovement(ClientInput input) {
        double velocity = input.isHolding("shift") ? SPRINT_VELOCITY : WALK_VELOCITY;
        if (this.isCarryingTheBall()) {
            velocity *= CARRYING_BALL_VELOCITY_MULTIPLIER;
        }

        this.setVelocity(input.getRequestedVelocity().times(velocity));

        if (this.isCarryingTheBall()) {
            if (input.isHolding("e")) {
                Player playerToPass = getPlayerToPass();
                pass(playerToPass);
            } else if (input.isHolding("r")) {
                shoot();
            }
        }
    }

    private void pass(Player player) {
        Translation2d delta = player.getPosition().minus(this.position);

        this.ball.kick(delta.times(1.5));
    }

    private void shoot() {
        this.ball.kick(new Translation2d(40, this.direction));
    }

    private Player getPlayerToPass() {
        return this.team.getPlayers().stream()
                .filter(p -> !p.equals(this))
                .min(Comparator.comparingDouble(p -> {
                    Translation2d delta = p.getPosition().minus(this.position);
                    double angleDiff = Math.abs(delta.getAngle().minus(this.direction).getRadians());

                    return 1 * angleDiff + 0.1 * delta.getNorm();
                }))
                .orElse(null);
    }

    @Override
    public void update() {
        this.position = this.position.plus(this.velocity.times(Constants.PERIOD));
    }
}
