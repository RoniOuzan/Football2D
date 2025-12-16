package com.football.client.inputs;

import com.football.client.json.InputPacket;
import com.football.client.keybinds.Keybind;
import com.football.game.strategy.TeamStrategy;
import com.football.game.players.Player;
import com.football.game.team.CameraPosition;
import com.football.util.math.geometry.Rotation2d;
import com.football.util.math.geometry.Translation2d;

public interface InputHandler {
    Translation2d getRequestedVelocity();

    default Translation2d getOrientedRequestedVelocity(CameraPosition camera, Player player) {
        Rotation2d orientation = camera == CameraPosition.BROADCAST ? Rotation2d.kZero : player.getDirection();
        return this.getRequestedVelocity().rotateBy(orientation.unaryMinus());
    }

    boolean isPressed(Keybind keybind);

    boolean isHolding(Keybind keybind);

    boolean isReleased(Keybind keybind);

    void updateInput(InputPacket.DevicePacket devicePacket);

    void runInputs(TeamStrategy teamStrategy, Player player);
}
