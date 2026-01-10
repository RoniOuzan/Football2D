package com.football.game;

import com.football.GameManager;
import com.football.game.players.Player;
import com.football.game.team.Team;
import com.football.util.math.MathUtil;
import com.football.util.math.geometry.Rotation2d;
import com.football.util.math.geometry.Translation2d;
import com.football.util.math.geometry.Translation3d;
import com.football.util.math.interpolation.TimeInterpolatableBuffer;

public class Ball {

    private static final double OFFSET_FROM_PLAYER = 0.75;

    // --- Physics constants ---
    // --- Ground ---
    public static final double FRICTION_ACCEL = -6; // much heavier than real life
    private static final double MIN_SPEED = 0.05;

    // --- Air ---
    private static final double GRAVITY = -9.81;        // slightly stronger than Earth
    private static final double AIR_DRAG = 0.5 * 1.225 * 0.25 * 0.11 * 0.11 * Math.PI;        // very important for FIFA feel
    private static final double SPIN_DRAG = 0.5 * 1.225 * 0.4 * 0.11 * 0.11 * Math.PI * 0.11;        // very important for FIFA feel
    private static final double MASS = 0.43; // kg
    private static final double SPIN_LOSS_PER_SECOND = 0.8;
    private static final double SPIN_LOSS_PER_ITERATION = Math.pow(SPIN_LOSS_PER_SECOND, GameManager.PERIOD);

    // --- Collisions ---
    private static final double BOUNCE_DAMPING = 0.55; // walls & posts
    private static final double GROUND_RESTITUTION = 0.32; // low bounces

    private static final long CARRY_COOLDOWN_MS = 300;
    private static final long DRIBBLE_COOLDOWN_MS = 200;

    private static final double RADIUS = 0.2;

    private transient final Game game;

    private Translation3d position;
    private Translation3d velocity;
    private Translation3d spin;
    private transient Player carrier = null;

    private transient long timeReleased;
    private transient long timeDribbled;

    private transient final TimeInterpolatableBuffer<Translation3d> positions = TimeInterpolatableBuffer.createBuffer(1);

    public Ball(Game game) {
        this.game = game;

        this.position = new Translation3d(0, 0, RADIUS);
        this.velocity = new Translation3d();
        this.spin = new Translation3d();

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
        return this.positions.getSample(this.game.getRealTime() - lookBackTime).orElse(null);
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
        }

        this.timeReleased = System.currentTimeMillis();
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
        return this.getPosition2d().getDistance(player.getPosition()) < OFFSET_FROM_PLAYER && this.position.getZ() <= Player.PLAYER_HEIGHT;
    }

    public void dribble(Translation3d velocity, Translation3d spin) {
        this.velocity = velocity;
        this.spin = spin;
    }

    public void kick(Translation3d velocity, Translation3d spin) {
        this.setCarrier(null);
        this.dribble(velocity, spin);
    }

    public void kick(Translation2d target, double finalPlanarVelocity, double heightScale) {
        kick(new Translation3d(target, RADIUS), finalPlanarVelocity, heightScale, new Translation3d());
    }

    public void kick(Translation3d target, double finalPlanarVelocity, double heightScale, Translation3d spin) {
        this.setCarrier(null);

        // --- Vector to target ---
        Translation3d diff = target.minus(this.position);
        Translation2d dir = diff.toTranslation2d().normalized();
        double planarDistance = diff.toTranslation2d().getNorm();
        double dz = diff.getZ();

        // --- Set spin ---
        this.spin = spin;

        // --- Estimate flight time (planar approximation) ---
        double initialVelocity = calculateInitialVelocity(planarDistance, finalPlanarVelocity, heightScale);
        double estimateTime = planarDistance / ((initialVelocity + finalPlanarVelocity) * 0.5);
        double effectiveTime = estimateTime * heightScale;

        // --- Predict lateral displacement from sidespin ---
        double lateralAccel = SPIN_DRAG * finalPlanarVelocity * Math.abs(spin.getZ()) / MASS;
        double sideDisplacement = 0.5 * lateralAccel * effectiveTime * effectiveTime;

        // --- Convert displacement to angle offset ---
        double angleOffsetRad = Math.atan2(sideDisplacement, planarDistance);

        // Apply opposite the spin to pre-compensate
        Translation2d shootDir = dir.rotateBy(Rotation2d.fromRadians(-Math.signum(spin.getZ()) * angleOffsetRad));

        // --- Compute vertical velocity ---
        double verticalSpeed = heightScale > 0 ? (dz / effectiveTime) - (0.5 * GRAVITY * effectiveTime) : 0;

        // --- Set initial velocity ---
        this.velocity = new Translation3d(
                shootDir.getX() * initialVelocity,
                shootDir.getY() * initialVelocity,
                verticalSpeed
        );
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
        applyAirPhysics();

        this.updateCarrier(team1);
        this.updateCarrier(team2);

        if (this.carrier != null) {
            this.spin.times(SPIN_LOSS_PER_ITERATION); // Reduce spin 2 times faster when with player
        }
        this.position = this.position.plus(this.velocity.times(GameManager.PERIOD));

        if (!goalPostCollision()) {
            wallCollision();
        }

        if (this.isOnGround()) {
            groundCollision();
            rollingDeceleration();
        }
    }

    private void applyAirPhysics() {
        double velocity = this.velocity.getNorm();

        // Gravity affects Z velocity
        Translation3d acceleration = new Translation3d(0, 0, GRAVITY);
        if (velocity > 0.1) {
            // air drag (small)
            Translation3d dragAccel = this.velocity.times(-AIR_DRAG * velocity).div(MASS); // Needs to be times v^2 and then divided by v, so it canceled to just times v
            acceleration = acceleration.plus(dragAccel);

            // Magnus effect (spin-induced force)
            Translation3d magnusDir = getWorldRelativeSpin().crossProduct(this.velocity);
            if (magnusDir.getNorm() > 0) {
                Translation3d magnusAccel = magnusDir.normalized().times(SPIN_DRAG * velocity * this.spin.getNorm()).div(MASS);
                acceleration = acceleration.plus(magnusAccel);
            }
        }
        this.spin.times(SPIN_LOSS_PER_ITERATION);
        this.velocity = this.velocity.plus(acceleration.times(GameManager.PERIOD));
    }

    private Translation3d getWorldRelativeSpin() {
        Translation3d forward = this.velocity.normalized();
        Translation3d right = new Translation3d(0,0,1).crossProduct(forward).normalized();
        Translation3d up = forward.crossProduct(right); // guaranteed orthogonal

        // Convert ball-relative spin to world spin
        return right.times(this.spin.getY())
                .plus(forward.times(this.spin.getX()))  // topspin/backspin
                .plus(up.times(this.spin.getZ()));
    }

    private void groundCollision() {
        this.position = new Translation3d(
                this.position.getX(),
                this.position.getY(),
                RADIUS
        );

        // Bounce only if falling
        if (this.velocity.getZ() < 0) {
            double restitution = GROUND_RESTITUTION;

            // --- SPIN EFFECT ON BOUNCE ---
            double topSpin = this.spin.getY();
            if (topSpin > 0) {
                // topspin → kill bounce (driven shots)
                restitution *= MathUtil.clamp(1 - (topSpin / 30), 0.1, 1.0);
            } else if (topSpin < 0) {
                // backspin → higher bounce (chips)
                restitution *= MathUtil.clamp(1 - (topSpin / 50), 1.0, 1.6);
            }

            this.velocity = new Translation3d(
                    this.velocity.getX(),
                    this.velocity.getY(),
                    -this.velocity.getZ() * restitution
            );

            // Kill tiny bounces
            if (Math.abs(this.velocity.getZ()) < MIN_SPEED) {
                this.velocity = new Translation3d(
                        this.velocity.getX(),
                        this.velocity.getY(),
                        0
                );
            }
        }
    }

    private void wallCollision() {
        double x = this.position.getX();
        double y = this.position.getY();
        double z = this.position.getZ();

        // Skip bounce inside goal area
        if (z <= Game.CROSSBAR_HEIGHT - RADIUS) {
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
        this.position = new Translation3d(newX, newY, this.position.getZ());
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
        Translation2d planarVel = this.velocity.toTranslation2d();

        double speed = planarVel.getNorm();
        if (speed <= 0) return;

        double friction = FRICTION_ACCEL;
        friction *= 1 - MathUtil.clamp(this.spin.getY() / 100, 0, 0.5);
        double newSpeed = speed + (friction * GameManager.PERIOD);
        if (newSpeed < MIN_SPEED)
            newSpeed = 0;

        Translation2d newPlanar = planarVel.normalized().times(newSpeed);

        this.velocity = new Translation3d(
                newPlanar.getX(),
                newPlanar.getY(),
                this.velocity.getZ()
        );
    }

    private static double stepSpeedGround(double speed, double dt) {
        if (speed <= 0) return 0;

        // Rolling friction (constant)
        speed += FRICTION_ACCEL * dt;
        if (speed < 0) speed = 0;

        return speed;
    }

    private static double stepSpeedAir(double speed, double dt) {
        if (speed <= 0) return 0;

        // Air drag (quadratic)
        double dragAccel = AIR_DRAG * speed * speed / MASS;
        speed -= dragAccel * dt;
        if (speed < 0) speed = 0;

        return speed;
    }

    private static double simulateDistance(double initialSpeed, double targetFinalSpeed, double heightFactor) {
        double speed = initialSpeed;
        double distance = 0;
        double dt = GameManager.PERIOD;

        while (speed > targetFinalSpeed) {
            distance += speed * dt;
            if (speed > targetFinalSpeed * heightFactor) {
                speed = stepSpeedGround(speed, dt);
            } else {
                speed = stepSpeedAir(speed, dt);
            }

            if (speed <= 0) break;
        }

        return distance;
    }

    public static double calculateInitialVelocity(double targetDistance, double targetFinalSpeed, double heightFactor) {
        double low = targetFinalSpeed;
        double high = 100; // reasonable upper bound, increase if needed

        for (int i = 0; i < 60; i++) { // ~1e-18 precision
            double mid = (low + high) * 0.5;
            double dist = simulateDistance(mid, targetFinalSpeed, heightFactor);

            if (dist < targetDistance) {
                low = mid;
            } else {
                high = mid;
            }
        }

        return (low + high) * 0.5;
    }
}
