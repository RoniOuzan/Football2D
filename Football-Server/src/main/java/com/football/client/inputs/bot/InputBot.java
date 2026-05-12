package com.football.client.inputs.bot;

import com.football.GameManager;
import com.football.client.inputs.InputDevice;
import com.football.client.json.InputPacket;
import com.football.client.keybinds.Keybind;
import com.football.client.keybinds.KeybindAction;
import com.football.game.Ball;
import com.football.game.players.Player;
import com.football.game.strategy.TeamStrategy;
import com.football.util.math.MathUtil;
import com.football.util.math.geometry.Translation2d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    public void updateInput(TeamStrategy strategy) {
        this.virtualButtons = new HashSet<>();
        this.requestedVelocity = new Translation2d(0, 0);

        this.calculateAI(strategy);

        // reset the charge counter
        this.chargeSeconds.keySet().removeIf(kb -> !this.virtualButtons.contains(kb.name()));

        InputPacket.DevicePacket botPacket = new InputPacket.DevicePacket();
        botPacket.buttons = this.virtualButtons;

        super.updateInput(botPacket);
    }

    private void calculateAI(TeamStrategy strategy) {
        Player chosen = strategy.getChosenPlayer();
        if (chosen == null) return;

        if (strategy.getTeam().hasBall()) {
            handleOffense(strategy, chosen);
        } else {
            handleDefense(strategy, chosen);
        }
    }

    private void handleOffense(TeamStrategy strategy, Player chosen) {
        double shootWeight = evaluateShootUtility(strategy, chosen);
        double passWeight = evaluatePassUtility(strategy, chosen);
        double dribbleWeight = evaluateDribbleUtility(strategy, chosen);


        if (shootWeight > passWeight && shootWeight > dribbleWeight) {
            // SHOOTING
            Translation2d enemyGoal = strategy.getTeam().getOpponent().getOwnGoalPosition();
            double distToGoal = chosen.getPosition().getDistance(enemyGoal);

            // Calculate power: max power at 20 units away.
            double targetSeconds = MathUtil.clamp(distToGoal / 20.0, 0.3, 1) * KeybindAction.MAX_HOLD_TIME;

            this.pressButton(Keybind.SHOOT, targetSeconds);
            this.requestedVelocity = enemyGoal.minus(chosen.getPosition()).normalized(); // Aim at goal

            if (shootWeight > 0.8) {
                this.pressButton(Keybind.DRIVEN);
            }
        } else if (passWeight > dribbleWeight && this.bestPassTarget != null) {
            // PASSING
            double distToMate = chosen.getPosition().getDistance(this.bestPassTarget.getPosition());

            // Calculate power: max power at 40 units away.
            // minSeconds ensures a minimum hold time (roughly 5 frames worth).
            double minSeconds = 5.0 / GameManager.FPS;
            double targetSeconds = Math.max(minSeconds, Math.min(1.0, distToMate / 40.0) * KeybindAction.MAX_HOLD_TIME);

            this.pressButton(Keybind.PASS, targetSeconds);
            this.requestedVelocity = this.bestPassTarget.getPosition().minus(chosen.getPosition()).normalized(); // Aim at teammate
        } else {
            // DRIBBLING
            Translation2d enemyGoal = strategy.getTeam().getOpponent().getOwnGoalPosition();
            this.requestedVelocity = enemyGoal.minus(chosen.getPosition()).normalized();

            if (dribbleWeight > 0.7) {
                this.pressButton(Keybind.SPRINT);
            }
        }
    }

    private void handleDefense(TeamStrategy strategy, Player chosen) {
        Player bestDefender = strategy.getTeam().getClosestPlayerToBall();
        if (bestDefender != null && !chosen.equals(bestDefender)) {
            strategy.setChosenPlayer(bestDefender);
        }

        // Move towards the ball's current position to close the gap
        // Using a smaller prediction (0.2s) or current position is better for high-pressure defense
        Translation2d targetPos = strategy.getBall().getPosition2d();
        this.requestedVelocity = targetPos.minus(chosen.getPosition()).normalized();
        this.pressButton(Keybind.SPRINT);
    }

    // --- Utility Functions (0.0 to 1.0) ---

    private double evaluateShootUtility(TeamStrategy strategy, Player chosen) {
        Translation2d enemyGoal = strategy.getTeam().getOpponent().getOwnGoalPosition();
        double distanceToGoal = chosen.getPosition().getDistance(enemyGoal);

        // Max range ~30 units.
        double score = Math.max(0, 1.0 - (distanceToGoal / 30.0));

        // Don't shoot if a defender is right in front of us
        if (getDistanceToNearestDefender(strategy, chosen) < 2.0) {
            score -= 0.3;
        }

        return Math.max(0, score);
    }

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
                double score = 0.5 + ((myDistToGoal - mateDistToGoal) / 40.0);

                // Penalize extremely long passes
                score -= (distToMate / 60.0);

                // Check if passing lane is intercepted by an opponent
                if (isPassingLaneBlocked(strategy, chosen.getPosition(), teammate.getPosition())) {
                    score -= 0.6; // Heavy penalty for risky passes
                }

                if (score > bestScore) {
                    bestScore = score;
                    this.bestPassTarget = teammate;
                }
            }
        }
        return Math.max(0, Math.min(1.0, bestScore));
    }

    private double evaluateDribbleUtility(TeamStrategy strategy, Player chosen) {
        double distToDefender = getDistanceToNearestDefender(strategy, chosen);

        // If the nearest defender is far away, dribbling is highly effective
        if (distToDefender > 10.0) return 0.9;
        if (distToDefender > 5.0) return 0.6;

        // If a defender is right in our face, we should probably pass or shoot
        return 0.2;
    }


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

    private boolean isPassingLaneBlocked(TeamStrategy strategy, Translation2d start, Translation2d end) {
        for (Player enemy : strategy.getTeam().getOpponent().getPlayers()) {
            // If an enemy is within 2 units of the line segment between the passer and receiver, it's blocked
            double distToLane = getDistanceToSegment(start, end, enemy.getPosition());
            if (distToLane < 2.0) {
                return true;
            }
        }
        return false;
    }

    // Standard vector math to find the shortest distance from a point to a line segment
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

    // Standard press for things like sprinting or switching players
    private void pressButton(Keybind keybind) {
        this.virtualButtons.add(keybind.name());
    }

    // Dynamic press for kicking (holds the button for X seconds to build power, then releases)
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