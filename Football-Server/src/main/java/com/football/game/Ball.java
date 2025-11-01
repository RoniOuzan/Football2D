package com.football.game;

import com.football.Constants;
import com.football.game.players.Player;
import com.football.util.math.geometry.Translation2d;

public class Ball implements Element {

    private static final Translation2d OFFSET_FROM_PLAYER = new Translation2d(1, 0);

    private Translation2d position;
    private Translation2d velocity;

    private transient Player carrier = null;

    public Ball() {
        this.position = new Translation2d();
        this.velocity = new Translation2d();
    }

    public Translation2d getPosition() {
        return position;
    }

    public Player getCarrier() {
        return carrier;
    }

    public void setCarrier(Player carrier) {
        if (carrier == null) {
            this.carrier.setCarryingTheBall(false);
        } else {
            carrier.setCarryingTheBall(true);
        }

        this.carrier = carrier;
    }

    public void setVelocity(Translation2d velocity) {
        this.velocity = velocity;
    }

    @Override
    public void update() {
        if (this.carrier != null) {
            this.velocity = this.carrier.getVelocity();
            this.position = this.carrier.getPosition().plus(OFFSET_FROM_PLAYER.rotateBy(this.carrier.getDirection()));
            return;
        }

        this.position = this.position.plus(this.velocity.times(Constants.PERIOD));

        double norm = this.velocity.getNorm() < 1 ? 0 : this.velocity.getNorm() * 0.9;
        this.velocity = new Translation2d(norm, this.velocity.getAngle());
    }
}
