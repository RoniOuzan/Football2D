package com.football.client;

import com.football.util.math.geometry.Translation2d;
import lombok.ToString;

import java.util.Map;

@ToString
public class InputController extends ClientInput {
    private static final Map<Keybind, String> keybinds = Map.of(
            Keybind.SWITCH_PLAYER, "LB",
            Keybind.SPRINT, "LT",
            Keybind.PASS, "A",
            Keybind.THROUGH, "Y",
            Keybind.SHOOT, "B"
    );

    public double leftX = 0;
    public double leftY = 0;
    public double rightX = 0;
    public double rightY = 0;
    public double LT = 0;
    public double RT = 0;

    public InputController(InputPacket.DevicePacket devicePacket) {
        super(keybinds, devicePacket);
    }

    @Override
    public Translation2d getRequestedVelocity() {
        Translation2d joy = new Translation2d(this.leftX, this.leftY);
        return joy.getNorm() < 0.05 ? new Translation2d() : joy.normalized();
    }

    @Override
    public void updateInput(InputPacket.DevicePacket devicePacket) {
        super.updateInput(devicePacket);

        this.leftX = devicePacket.leftX;
        this.leftY = devicePacket.leftY;
        this.rightX = devicePacket.rightX;
        this.rightY = devicePacket.rightY;
        this.LT = devicePacket.LT;
        this.RT = devicePacket.RT;
    }
}
