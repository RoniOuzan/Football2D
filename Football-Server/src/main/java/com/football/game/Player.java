package com.football.game;

import com.football.Constants;
import com.football.util.math.geometry.Translation2d;

public class Player implements Element {
    private Translation2d position;
    private Translation2d velocity;

    public Player() {
        this.position = new Translation2d();
        this.velocity = new Translation2d();
    }

    public void move(Translation2d velocity) {
        this.velocity = velocity;
    }

    @Override
    public void update() {
        this.position = this.position.plus(this.velocity.times(Constants.PERIOD));
    }
}
