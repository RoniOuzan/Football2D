package com.football.client.inputs.devices;

import com.football.client.inputs.InputDevice;
import com.football.client.json.InputPacket;
import com.football.util.math.geometry.Translation2d;

import java.util.HashMap;

public class InputNullDevice extends InputDevice {
    public InputNullDevice() {
        super(new HashMap<>(), new InputPacket.DevicePacket());
    }

    @Override
    public Translation2d getRequestedVelocity() {
        return new Translation2d();
    }
}
