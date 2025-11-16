package com.football.game.players;

import com.football.game.Ball;
import com.football.game.Team;
import com.football.util.math.geometry.Translation2d;

public class Attacker extends Player {

    private static final double ATTACK_SPEED = 8;
    private static final double DEFEND_SPEED = 5;
    private static final double COUNTER_ATTACK_SPEED = 10;
    private static final double POSSESSION_SPEED = 6;

    public Attacker(Team team, Ball ball, Translation2d position) {
        super(team, ball, position);
    }
}
