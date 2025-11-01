package com.football.game.players;

import com.football.game.Team;
import com.football.util.math.geometry.Translation2d;

public class Goalkeeper extends Player {
    public Goalkeeper(Team team, Translation2d position) {
        super(team, position);
    }

    @Override
    public void handleMovement(Team team) {
        setVelocity(new Translation2d());
    }
}
