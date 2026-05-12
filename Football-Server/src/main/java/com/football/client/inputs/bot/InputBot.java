package com.football.client.inputs.bot;

import com.football.GameManager;
import com.football.client.inputs.InputDevice;
import com.football.client.json.InputPacket;
import com.football.client.keybinds.Keybind;
import com.football.client.keybinds.KeybindAction;
import com.football.game.players.Player;
import com.football.game.strategy.TeamStrategy;
import com.football.util.math.MathUtil;
import com.football.util.math.geometry.Translation2d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * AI implementation of an {@link InputDevice}.
 * This bot evaluates the game state using a utility-based system to decide
 * whether to shoot, pass, or dribble when in possession, and how to intercept
 * the ball when defending.
 */
public class InputBot extends InputDevice {

    private Translation2d requestedVelocity = new Translation2d(0, 0);
    private Set<String> virtualButtons = new HashSet<>();

    // Map to track how long we've been holding a button for kick power
    private final Map<Keybind, Double> chargeSeconds = new HashMap<>();

    // Store this so we know who to aim at if we decide to pass
    private Player bestPassTarget = null;

    public InputBot() {
        super(getKeybindMap(), InputPacket.EMPTY_DEVICE);
    }

    @Override
    public Translation2d getRequestedVelocity() {
        return this.requestedVelocity;
    }

    /**
     * The main entry point for the bot's logic. Called every frame by the GameManager.
     * Resets inputs, calculates new AI decisions, and updates the device packet.
     * @param strategy The current tactical state of the team.
     */
    public void updateInput(TeamStrategy strategy) {
        // Resets
        this.virtualButtons = new HashSet<>();
        this.requestedVelocity = new Translation2d(0, 0);

        // Calculation
        this.calculateAI(strategy);

        // Reset the charge counter
        this.chargeSeconds.keySet().removeIf(kb -> !this.virtualButtons.contains(kb.name()));

        InputPacket.DevicePacket botPacket = new InputPacket.DevicePacket();
        botPacket.buttons = this.virtualButtons;

        super.updateInput(botPacket);
    }

    /**
     * Determines whether the bot should act offensively or defensively
     * based on team ball possession.
     */
    private void calculateAI(TeamStrategy strategy) {
        Player chosen = strategy.getChosenPlayer();
        if (chosen == null) return;

        if (strategy.getTeam().hasBall()) {
            handleOffense(strategy, chosen);
        } else {
            handleDefense(strategy, chosen);
        }
    }

    /**
     * Handles logic when the team has the ball. Evaluates weights for
     * shooting, passing, and dribbling to choose the best action.
     */
    private void handleOffense(TeamStrategy strategy, Player chosen) {
        double shootWeight = evaluateShootUtility(strategy, chosen);
        double passWeight = evaluatePassUtility(strategy, chosen);
        double dribbleWeight = evaluateDribbleUtility(strategy, chosen);
        
        if (shootWeight > passWeight && shootWeight > dribbleWeight) {
            // SHOOTING
            Translation2d enemyGoal = strategy.getTeam().getOpponent().getOwnGoalPosition();
            double distToGoal = chosen.getPosition().getDistance(enemyGoal);

            // Calculate power
            double powerSeconds = MathUtil.clamp(distToGoal / BotConstants.MAX_POWER_SHOOT_DISTANCE, BotConstants.MIN_POWER_SHOOT_PERCENT, 1) * KeybindAction.MAX_HOLD_TIME;

            this.pressButton(Keybind.SHOOT, powerSeconds);
            this.requestedVelocity = enemyGoal.minus(chosen.getPosition()).normalized(); // Aim at goal

            if (shootWeight > BotConstants.DRIVEN_SHOT_WEIGHT) {
                this.pressButton(Keybind.DRIVEN);
            }
        } else if (passWeight > dribbleWeight && this.bestPassTarget != null) {
            // PASSING
            double distToMate = chosen.getPosition().getDistance(this.bestPassTarget.getPosition());

            double powerSeconds = MathUtil.clamp(distToMate / BotConstants.MAX_POWER_PASSING_DISTANCE, BotConstants.MIN_POWER_PASSING_PERCENT, 1) * KeybindAction.MAX_HOLD_TIME;

            this.pressButton(Keybind.PASS, powerSeconds);
            this.requestedVelocity = this.bestPassTarget.getPosition().minus(chosen.getPosition()).normalized(); // Aim at teammate
        } else {
            // DRIBBLING
            Translation2d enemyGoal = strategy.getTeam().getOpponent().getOwnGoalPosition();
            this.requestedVelocity = enemyGoal.minus(chosen.getPosition()).normalized();

            if (dribbleWeight > BotConstants.DRIBBLE_WEIGHT_THRESHOLD_TO_SPRING) {
                this.pressButton(Keybind.SPRINT);
            }
        }
    }

    /**
     * Handles logic when the opponent has the ball. Focuses on switching
     * to the closest defender and closing the gap to the ball.
     */
    private void handleDefense(TeamStrategy strategy, Player chosen) {
        Player bestPlayer = strategy.getTeam().getClosestPlayerToBall();
        if (bestPlayer != null && !chosen.equals(bestPlayer)) {
            strategy.setChosenPlayer(bestPlayer);
        }

        // Move towards the ball's position to close the gap
        Translation2d targetPos = strategy.getBall().getPosition2d();
        this.requestedVelocity = targetPos.minus(chosen.getPosition()).normalized();
        this.pressButton(Keybind.SPRINT);
    }

    // --- Utility Functions (0.0 to 1.0) ---

    /**
     * Calculates how viable a shot is.
     * Score increases as the player gets closer to the goal and
     * decreases if defenders are nearby.
     */
    private double evaluateShootUtility(TeamStrategy strategy, Player chosen) {
        Translation2d enemyGoal = strategy.getTeam().getOpponent().getOwnGoalPosition();
        double distanceToGoal = chosen.getPosition().getDistance(enemyGoal);

        double score = 1.0 - (distanceToGoal / BotConstants.MAX_SHOOTING_DISTANCE);

        // Don't shoot if a defender is near of us
        if (getDistanceToNearestDefender(strategy, chosen) < BotConstants.DEFENDER_CLOSE_DISTANCE_THRESHOLD) {
            score -= BotConstants.DEFENDER_CLOSE_PENALTY;
        }

        return Math.max(0, score);
    }

    /**
     * Iterates through teammates to find the best passing option.
     * Factors in advancement (moving the ball forward), distance, and 
     * interception risks.
     */
    private double evaluatePassUtility(TeamStrategy strategy, Player chosen) {
        double bestScore = 0.0;
        this.bestPassTarget = null;

        Translation2d enemyGoal = strategy.getTeam().getOpponent().getOwnGoalPosition();
        double myDistToGoal = chosen.getPosition().getDistance(enemyGoal);

        for (Player teammate : strategy.getTeam().getPlayers()) {
            if (teammate.equals(chosen)) continue;

            double mateDistToGoal = teammate.getPosition().getDistance(enemyGoal);
            double distToMate = chosen.getPosition().getDistance(teammate.getPosition());

            // Only consider passes that advance the ball (or if we are desperate)
            if (mateDistToGoal < myDistToGoal) {
                // Base score relies on how far forward the teammate is
                double score = BotConstants.PASS_BASE_SCORE + ((myDistToGoal - mateDistToGoal) / BotConstants.PASS_ADVANCEMENT_DIVISOR);

                // Penalize extremely long passes
                score -= (distToMate / BotConstants.PASS_DISTANCE_PENALTY_DIVISOR);

                // Check if passing lane is intercepted by an opponent
                if (isPassingLaneBlocked(strategy, chosen.getPosition(), teammate.getPosition())) {
                    score -= BotConstants.PASS_RISK_PENALTY; // Heavy penalty for risky passes
                }

                if (score > bestScore) {
                    bestScore = score;
                    this.bestPassTarget = teammate;
                }
            }
        }
        return MathUtil.clamp(bestScore, 0, 1);
    }

    /**
     * Calculates the utility of keeping the ball.
     * High utility if there is open space, low utility if under 
     * immediate pressure.
     */
    private double evaluateDribbleUtility(TeamStrategy strategy, Player chosen) {
        double distToDefender = getDistanceToNearestDefender(strategy, chosen);

        // If the nearest defender is far away, dribbling is highly effective
        if (distToDefender > BotConstants.DRIBBLE_SAFE_DISTANCE_THRESHOLD) return BotConstants.DRIBBLE_UTILITY_SAFE;
        if (distToDefender > BotConstants.DRIBBLE_PRESSURE_DISTANCE_THRESHOLD) return BotConstants.DRIBBLE_UTILITY_PRESSURE;

        // If a defender is right in our face, we should probably pass or shoot
        return BotConstants.DRIBBLE_UTILITY_LOCKED_DOWN;
    }

    /**
     * Finds the distance from the chosen player to the nearest 
     * opponent on the field.
     */
    private double getDistanceToNearestDefender(TeamStrategy strategy, Player chosen) {
        double minDistance = Double.MAX_VALUE;
        for (Player enemy : strategy.getTeam().getOpponent().getPlayers()) {
            double dist = chosen.getPosition().getDistance(enemy.getPosition());
            if (dist < minDistance) {
                minDistance = dist;
            }
        }
        return minDistance;
    }

    /**
     * Checks if any opponent is close enough to the imaginary line 
     * between the passer and receiver to potentially intercept.
     */
    private boolean isPassingLaneBlocked(TeamStrategy strategy, Translation2d start, Translation2d end) {
        for (Player enemy : strategy.getTeam().getOpponent().getPlayers()) {
            // If an enemy is within 2 units of the line segment between the passer and receiver, it's blocked
            double distToLane = getDistanceToSegment(start, end, enemy.getPosition());
            if (distToLane < BotConstants.PASSING_LANE_INTERCEPT_RADIUS) {
                return true;
            }
        }
        return false;
    }

    /**
     * Standard vector math to find the shortest distance from a point P to a line segment AB.
     */
    private double getDistanceToSegment(Translation2d A, Translation2d B, Translation2d P) {
        Translation2d AB = B.minus(A);
        Translation2d AP = P.minus(A);

        double lengthSquaredAB = AB.getX() * AB.getX() + AB.getY() * AB.getY();
        if (lengthSquaredAB == 0) return P.getDistance(A); // A and B are the same point

        // Dot product to find projection scalar
        double t = (AP.getX() * AB.getX() + AP.getY() * AB.getY()) / lengthSquaredAB;

        // Clamp to segment [0, 1]
        t = Math.max(0, Math.min(1, t));

        Translation2d projection = new Translation2d(
                A.getX() + t * AB.getX(),
                A.getY() + t * AB.getY()
        );

        return P.getDistance(projection);
    }


    // --- Press Button Logic ---

    /**
     * Sets a button as pressed for the current frame.
     * Used for binary states like Sprinting.
     */
    private void pressButton(Keybind keybind) {
        this.virtualButtons.add(keybind.name());
    }

    /**
     * Manages a timed button press to simulate "charging" power.
     * The button will be held until targetSeconds is reached, then released.
     * 
     * @param keybind The button to hold.
     * @param targetSeconds Total duration to hold the button for.
     */
    private void pressButton(Keybind keybind, double targetSeconds) {
        double currentSeconds = this.chargeSeconds.getOrDefault(keybind, 0.0);

        if (currentSeconds >= targetSeconds) {
            // We've held it long enough to build the right power.
            // Force the release by NOT adding it to virtualButtons, and reset the counter.
            this.chargeSeconds.put(keybind, 0.0);
        } else {
            // Keep holding it to build more power. We add the time of one frame (1/FPS)
            this.chargeSeconds.put(keybind, currentSeconds + (1.0 / GameManager.FPS));
            this.virtualButtons.add(keybind.name());
        }
    }

    private static Map<Keybind, String> getKeybindMap() {
        Map<Keybind, String> keybinds = new HashMap<>();
        for (Keybind keybind : Keybind.values()) {
            keybinds.put(keybind, keybind.name());
        }
        return keybinds;
    }
}