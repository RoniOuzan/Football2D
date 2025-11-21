package com.football.game;

import com.football.Constants;
import com.football.game.players.Player;
import com.football.util.math.geometry.Translation2d;
import com.football.util.math.interpolation.TimeInterpolatableBuffer;

public class Ball {

    private static final Translation2d OFFSET_FROM_PLAYER = new Translation2d(1, 0);

    // --- Physics constants ---
    private static final double FRICTION = 6.0;      // m/s², slows the ball
    private static final double MIN_SPEED = 0.05;    // below this -> stop completely
    private static final double BOUNCE_DAMPING = 0.7; // energy loss on wall bounce

    private static final double PICK_UP_BALL_THRESHOLD = 1;
    private static final long CARRY_COOLDOWN_MS = 300;

    private transient final Game game;

    private Translation2d position;
    private Translation2d velocity;
    private transient Player carrier = null;

    private transient long timeReleased;

    private transient final TimeInterpolatableBuffer<Translation2d> positions = TimeInterpolatableBuffer.createBuffer(1);

    public Ball(Game game) {
        this.game = game;

        this.position = new Translation2d();
        this.velocity = new Translation2d();

        this.timeReleased = System.currentTimeMillis();
    }

    public Translation2d getPosition() {
        return position;
    }

    public Translation2d getPosition(double lookBackTime) {
        return this.positions.getSample(this.game.getMatchTime() - lookBackTime).orElse(null);
    }

    public Translation2d getPredictedPosition(double seconds) {
        return this.position.plus(this.velocity.times(seconds));
    }

    public Translation2d getVelocity() {
        return velocity;
    }

    public Player getCarrier() {
        return carrier;
    }

    public void setCarrier(Player carrier) {
        this.carrier = carrier;

        if (carrier != null) {
            this.velocity = carrier.getVelocity(); // reset velocity while carried
        } else {
            this.timeReleased = System.currentTimeMillis();
        }
    }

    public void update() {
        this.position = this.position.plus(this.velocity.times(Constants.PERIOD));

        if (this.carrier != null) {
            // Ball follows player slightly in front of their facing direction
            this.velocity = this.carrier.getVelocity();
            this.position = this.carrier.getPosition()
                    .plus(OFFSET_FROM_PLAYER.rotateBy(this.carrier.getDirection()));
        } else {
            wallCollision();
            rollingDeceleration();
        }

        this.positions.addSample(this.game.getMatchTime(), this.position);
    }

    private void wallCollision() {
        // Handle wall collisions (simple bounce)
        double x = this.position.getX();
        double y = this.position.getY();

        if (x < -Game.MAX_X) {
            x = -Game.MAX_X;
            this.velocity = new Translation2d(-this.velocity.getX() * BOUNCE_DAMPING, this.velocity.getY() * BOUNCE_DAMPING);
        } else if (x > Game.MAX_X) {
            x = Game.MAX_X;
            this.velocity = new Translation2d(-this.velocity.getX() * BOUNCE_DAMPING, this.velocity.getY() * BOUNCE_DAMPING);
        }

        if (y < -Game.MAX_Y) {
            y = -Game.MAX_Y;
            this.velocity = new Translation2d(this.velocity.getX() * BOUNCE_DAMPING, -this.velocity.getY() * BOUNCE_DAMPING);
        } else if (y > Game.MAX_Y) {
            y = Game.MAX_Y;
            this.velocity = new Translation2d(this.velocity.getX() * BOUNCE_DAMPING, -this.velocity.getY() * BOUNCE_DAMPING);
        }

        this.position = new Translation2d(x, y);
    }

    private void rollingDeceleration() {
        double speed = this.velocity.getNorm();
        if (speed > 0) {
            double newSpeed = Math.max(speed - (FRICTION * Constants.PERIOD), 0);
            if (newSpeed < MIN_SPEED) newSpeed = 0;

            this.velocity = (newSpeed == 0)
                    ? new Translation2d()
                    : this.velocity.normalized().times(newSpeed);
        }
    }

    public boolean shouldBePickedUpBy(Player player) {
        if (System.currentTimeMillis() - this.timeReleased < CARRY_COOLDOWN_MS) {
            return false;
        }

        return this.position.getDistance(player.getPosition()) < PICK_UP_BALL_THRESHOLD;
    }

    // --- Kick mechanic ---
    public void kick(Translation2d power) {
        this.setCarrier(null);
        this.velocity = power;
    }

    // --- Utility: check if moving ---
    public boolean isMoving() {
        return this.velocity.getNorm() > MIN_SPEED;
    }
}
