package com.football.client.inputs;

import com.football.client.keybinds.Keybind;
import com.football.game.strategy.TeamStrategy;
import com.football.game.players.Player;
import com.football.util.math.geometry.Translation2d;

public interface InputHandler {
    Translation2d getRequestedVelocity();

    boolean isPressed(Keybind keybind);

    boolean isHolding(Keybind keybind);

    boolean isReleased(Keybind keybind);

    void updateInput(InputPacket.DevicePacket devicePacket);

    void runInputs(TeamStrategy teamStrategy, Player player);
}
