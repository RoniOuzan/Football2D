package com.football.game;

import com.football.GameManager;
import com.football.game.players.Player;
import com.football.util.math.MathUtil;
import com.football.util.math.geometry.Translation2d;
import com.football.util.math.geometry.Translation3d;
import com.football.util.math.interpolation.TimeInterpolatableBuffer;

public class Ball {

    private static final double OFFSET_FROM_PLAYER = 0.75;

    // --- Physics constants ---
    public static final double FRICTION_ACCEL = -6.0;      // m/s², slows the ball
    private static final double MIN_SPEED = 0.05;    // below this -> stop completely
    private static final double BOUNCE_DAMPING = 0.7; // energy loss on wall bounce

    public static final double GRAVITY = -9.81;      // m/s²
    public static final double GROUND_RESTITUTION = 0.45; // vertical bounce
    public static final double AIR_DRAG = 0.15;      // optional, mild damping

    private static final long CARRY_COOLDOWN_MS = 300;

    private static final double RADIUS = 0.2;

    private transient final Game game;

    private Translation3d position;
    private Translation3d velocity;
    private transient Player carrier = null;

    private transient long timeReleased;

    private transient final TimeInterpolatableBuffer<Translation3d> positions = TimeInterpolatableBuffer.createBuffer(1);

    public Ball(Game game) {
        this.game = game;

        this.position = new Translation3d(0, 0, RADIUS);
        this.velocity = new Translation3d();

        this.timeReleased = System.currentTimeMillis();

        this.positions.addSample(0, this.position);
    }

    public Translation3d getPosition() {
        return this.position;
    }

    public Translation2d getPosition2d() {
        return this.position.toTranslation2d();
    }

    public Translation3d getPosition(double lookBackTime) {
        return this.positions.getSample(this.game.getMatchTime() - lookBackTime).orElse(null);
    }

    public Translation3d getPredictedPosition(double seconds) {
        return this.position.plus(this.velocity.times(seconds));
    }

    public Translation3d getVelocity() {
        return this.velocity;
    }

    public Translation2d getVelocity2d() {
        return this.velocity.toTranslation2d();
    }

    public Player getCarrier() {
        return carrier;
    }

    public void setCarrier(Player carrier) {
        this.carrier = carrier;

        if (carrier != null) {
            this.velocity = new Translation3d(carrier.getVelocity()); // reset velocity while carried
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

    public boolean shouldBePickedUpBy(Player player) {
        if (System.currentTimeMillis() - this.timeReleased < CARRY_COOLDOWN_MS)
            return false;
        return this.position.getDistance(player.getPosition()) < OFFSET_FROM_PLAYER;
    }

    public void kick(Translation3d velocity) {
        this.setCarrier(null);
        this.velocity = velocity;
    }

    public void kick(Translation3d target, double finalVelocity) {
        this.setCarrier(null);

        Translation3d diff = target.minus(this.position);
        this.velocity = diff.normalized().times(calculateInitialVelocity(finalVelocity, diff.toTranslation2d().getNorm()));
    }

    public int isAtGoal() {
        if (this.position.getZ() > Game.CROSSBAR_HEIGHT - RADIUS)
            return 0;

        if (Math.abs(this.position.getY()) > Game.GOAL_WIDTH / 2 - RADIUS)
            return 0;

        if (this.position.getX() > Game.MAX_X + RADIUS)
            return 1;
        if (this.position.getX() < -(Game.MAX_X + RADIUS))
            return -1;

        return 0;
    }

    private boolean isOnGround() {
        return this.position.getZ() <= RADIUS + 1e-3;
    }

    public void reset() {
        this.position = new Translation3d(0, 0, RADIUS);
        this.velocity = new Translation3d();

        this.carrier = null;
        this.timeReleased = System.currentTimeMillis();
    }

    public void update(Team team1, Team team2) {
        // 1. Apply gravity / air physics
        applyAirPhysics();

        this.updateCarrier(team1);
        this.updateCarrier(team2);

        this.position = this.position.plus(this.velocity.times(GameManager.PERIOD));

        if (this.carrier != null) {
            this.velocity = new Translation3d(this.carrier.getVelocity());
            this.position = new Translation3d(
                    this.carrier.getPosition()
                            .plus(new Translation2d(OFFSET_FROM_PLAYER, this.carrier.getDirection())),
                    RADIUS
            );
        } else {
            // 3. Ground collision
            groundCollision();

            // 4. Goal / wall collisions (planar)
            if (!goalPostCollision()) {
                wallCollision();
            }

            // 5. Rolling friction (ground only)
            rollingDeceleration();
        }
    }

    private void applyAirPhysics() {
        if (this.carrier != null) return;

        // Gravity affects Z velocity
        this.velocity = new Translation3d(
                this.velocity.getX(),
                this.velocity.getY(),
                this.velocity.getZ() + GRAVITY * GameManager.PERIOD
        );

        // air drag (small)
        this.velocity = this.velocity.times(1.0 - AIR_DRAG * GameManager.PERIOD);
    }

    private void groundCollision() {
        if (this.isOnGround()) {
            this.position = new Translation3d(
                    this.position.getX(),
                    this.position.getY(),
                    RADIUS
            );

            // Bounce only if falling
            if (this.velocity.getZ() < 0) {
                this.velocity = new Translation3d(
                        this.velocity.getX(),
                        this.velocity.getY(),
                        -this.velocity.getZ() * GROUND_RESTITUTION
                );

                // Kill tiny bounces
                if (Math.abs(this.velocity.getZ()) < 0.5) {
                    this.velocity = new Translation3d(
                            this.velocity.getX(),
                            this.velocity.getY(),
                            0
                    );
                }
            }
        }
    }

    private void wallCollision() {
        double x = this.position.getX();
        double y = this.position.getY();
        double z = this.position.getZ();

        // Skip bounce inside goal area
        boolean behindGoalLine = Math.abs(x) > Game.MAX_X + RADIUS;
        boolean insideGoalWidth = Math.abs(y) <= Game.GOAL_WIDTH / 2 + RADIUS;
        if (behindGoalLine && insideGoalWidth) {
            // If hitting the side net
            if (Math.abs(y) > Game.GOAL_WIDTH / 2 - RADIUS) {
                y = MathUtil.clamp(y, -(Game.GOAL_WIDTH / 2 - RADIUS), (Game.GOAL_WIDTH / 2 - RADIUS));
                this.velocity = new Translation3d(this.velocity.getX(), 0, this.velocity.getZ());
            }
            // If hitting back of the net
            if (Math.abs(x) > Game.MAX_X + Game.GOAL_DEPTH - RADIUS) {
                x = MathUtil.clamp(x, -(Game.MAX_X + Game.GOAL_DEPTH - RADIUS), (Game.MAX_X + Game.GOAL_DEPTH - RADIUS));
                this.velocity = new Translation3d(); // Stop in the net
            }

            this.position = new Translation3d(x, y, z);
            return;
        }

        if (insideGoalWidth) return;

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
        this.position = new Translation3d(newX, newY, RADIUS);
        this.velocity = reflect(this.velocity, normal).times(BOUNCE_DAMPING);
    }

    /** Reflect vector v across a given surface normal (must be normalized). */
    private Translation3d reflect(Translation3d v, Translation2d normal) {
        double dot = v.toTranslation2d().dot(normal); // projection length
        Translation2d velocity = v.toTranslation2d().minus(normal.times(2 * dot));
        return new Translation3d(
                velocity.getX(),
                velocity.getY(),
                this.velocity.getZ()
        );
    }

    private boolean goalPostCollision() {
        return Game.POSTS.stream().anyMatch(this::handlePost);
    }

    private boolean handlePost(Translation2d post) {
        if (this.position.getZ() > Game.CROSSBAR_HEIGHT + RADIUS) return false;

        Translation2d diff = this.position.toTranslation2d().minus(post);
        double dist = diff.getNorm();
        double minDist = Game.POST_RADIUS + RADIUS;

        if (dist >= minDist) return false; // no collision

        // --- Step 1: correct position so the ball is no longer penetrating ---
        double penetration = minDist - dist;
        Translation2d normal = diff.normalized();

        // push ball out of the post
        this.position = this.position.plus(new Translation3d(normal.times(penetration)));

        // --- Step 2: reflect velocity using circle normal ---
        this.velocity = reflect(this.velocity, normal).times(BOUNCE_DAMPING);
        return true;
    }

    private void rollingDeceleration() {
        if (!isOnGround()) return;

        Translation2d planarVel = this.velocity.toTranslation2d();

        double speed = planarVel.getNorm();
        if (speed <= 0) return;

        double newSpeed = Math.max(speed + (FRICTION_ACCEL * GameManager.PERIOD), 0);
        if (newSpeed < MIN_SPEED) newSpeed = 0;

        Translation2d newPlanar = planarVel.normalized().times(newSpeed);

        this.velocity = new Translation3d(
                newPlanar.getX(),
                newPlanar.getY(),
                this.velocity.getZ()
        );
    }

    public static double calculateInitialVelocity(double finalVelocity, double distance) {
        return Math.sqrt(Math.pow(finalVelocity, 2) - 2 * distance * FRICTION_ACCEL);
    }
}
