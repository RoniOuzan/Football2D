package com.football.game;

import com.football.Constants;
import com.football.game.players.Player;
import com.football.util.math.MathUtil;
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

    private static final double RADIUS = 0.35;

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

        this.positions.addSample(0, this.position);
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

    private void updateCarrier(Team team) {
        if (!team.hasBall()) {
            Player closest = team.getClosestPlayerToBall();

            if (this.shouldBePickedUpBy(closest)) {
                this.setCarrier(closest);
            }
        }
    }

    public int isAtGoal() {
        if (Math.abs(this.position.getY()) > Game.GOAL_WIDTH / 2 - RADIUS) return 0;

        if (this.position.getX() >= Game.MAX_X + RADIUS) {
            return 1;
        } else if (this.position.getX() <= -(Game.MAX_X + RADIUS)) {
            return -1;
        }
        return 0;
    }

    public void reset() {
        this.position = new Translation2d();
        this.velocity = new Translation2d();

        this.carrier = null;
        this.timeReleased = System.currentTimeMillis();
    }

    public void update(Team team1, Team team2) {
        this.updateCarrier(team1);
        this.updateCarrier(team2);

        this.position = this.position.plus(this.velocity.times(Constants.PERIOD));

        if (this.carrier != null) {
            // Ball follows player slightly in front of their facing direction
            this.velocity = this.carrier.getVelocity();
            this.position = this.carrier.getPosition()
                    .plus(OFFSET_FROM_PLAYER.rotateBy(this.carrier.getDirection()));
        } else {
            if (!goalPostCollision()) {
                wallCollision();
            }
            rollingDeceleration();
        }

        this.positions.addSample(this.game.getMatchTime(), this.position);
    }

    private void wallCollision() {
        double x = this.position.getX();
        double y = this.position.getY();

        // Skip bounce inside goal area (you already handle this)
        if (Math.abs(y) <= Game.GOAL_WIDTH / 2 - RADIUS && Math.abs(x) > Game.MAX_X - RADIUS) {
            if (Math.abs(y) > Game.GOAL_WIDTH / 2 - RADIUS) {
                y = MathUtil.clamp(y, -(Game.GOAL_WIDTH / 2 - RADIUS), Game.GOAL_WIDTH / 2 - RADIUS);
                this.velocity = new Translation2d(this.velocity.getX(), 0);
            }
            if (Math.abs(x) > Game.MAX_X + Game.GOAL_DEPTH - RADIUS) {
                x = MathUtil.clamp(x, -(Game.MAX_X + Game.GOAL_DEPTH - RADIUS), Game.MAX_X + Game.GOAL_DEPTH - RADIUS);
                this.velocity = new Translation2d();
            }

            this.position = new Translation2d(x, y);
            return;
        }

        if (x - RADIUS < -Game.MAX_X) { // Left wall
            wallBounce(-Game.MAX_X + RADIUS, y, new Translation2d(1, 0)); // normal points right
        } else if (x + RADIUS > Game.MAX_X) { // Right wall
            wallBounce(Game.MAX_X - RADIUS, y, new Translation2d(-1, 0)); // normal points left
        } else if (y - RADIUS < -Game.MAX_Y) { // Bottom wall
            wallBounce(x, -Game.MAX_Y + RADIUS, new Translation2d(0, 1)); // normal points up
        } else if (y + RADIUS > Game.MAX_Y) { // Top wall
            wallBounce(x, Game.MAX_Y - RADIUS, new Translation2d(0, -1)); // normal points down
        }
    }

    private void wallBounce(double newX, double newY, Translation2d normal) {
        this.position = new Translation2d(newX, newY);
        this.velocity = reflect(this.velocity, normal).times(BOUNCE_DAMPING);
    }

    /** Reflect vector v across a given surface normal (must be normalized). */
    private Translation2d reflect(Translation2d v, Translation2d normal) {
        double dot = v.dot(normal); // projection length
        return v.minus(normal.times(2 * dot));
    }

    private boolean goalPostCollision() {
        return Game.POSTS.stream().anyMatch(this::handlePost);
    }

    private boolean handlePost(Translation2d post) {
        Translation2d diff = this.position.minus(post);
        double dist = diff.getNorm();
        double minDist = Game.POST_RADIUS + RADIUS;

        if (dist >= minDist) return false; // no collision

        // --- Step 1: correct position so the ball is no longer penetrating ---
        double penetration = minDist - dist;
        Translation2d normal = diff.normalized();

        // push ball out of the post
        this.position = this.position.plus(normal.times(penetration));

        // --- Step 2: reflect velocity using circle normal ---
        this.velocity = reflect(this.velocity, normal).times(BOUNCE_DAMPING);
        return true;
    }

    private void rollingDeceleration() {
        if (this.velocity.getNorm() > 0) {
            double newSpeed = Math.max(this.velocity.getNorm() - (FRICTION * Constants.PERIOD), 0);
            if (newSpeed < MIN_SPEED)
                newSpeed = 0;

            this.velocity = this.velocity.normalized().times(newSpeed);
        }
    }

    public boolean shouldBePickedUpBy(Player player) {
        if (System.currentTimeMillis() - this.timeReleased < CARRY_COOLDOWN_MS)
            return false;
        return this.position.getDistance(player.getPosition()) < PICK_UP_BALL_THRESHOLD;
    }

    public void kick(Translation2d power) {
        this.setCarrier(null);
        this.velocity = power;
    }
}
