package com.football.game.players;

import com.football.game.Ball;
import com.football.game.BotResult;
import com.football.game.Team;
import com.football.util.math.geometry.Translation2d;

public class Goalkeeper extends Player {

    public Goalkeeper(Team team, Ball ball, Translation2d position) {
        super(team, ball, position);
    }

}

