package com.football.client;

import com.football.PacketHandler;
import com.football.client.inputs.*;
import com.football.client.inputs.devices.InputControllerDevice;
import com.football.client.inputs.devices.InputKeyboardDevice;
import com.football.client.json.InputPacket;
import org.eclipse.jetty.websocket.api.Session;

import java.util.ArrayList;
import java.util.List;

public class Client {
    private transient final Session session;

    private transient final List<InputDevice> inputs;
    private transient final AllInputDevices allInputDevices;

    private transient final PacketHandler packetHandler;

    public Client(Session session) {
        this.session = session;

        this.inputs = new ArrayList<>();
        this.allInputDevices = new AllInputDevices(this.inputs);

        this.packetHandler = new PacketHandler();
    }

    public List<InputDevice> getInputs() {
        return this.inputs;
    }

    public InputHandler getInput(int slot) {
        if (slot == -1)
            return this.allInputDevices;
        if (slot >= this.inputs.size())
            return null;
        return this.inputs.get(slot);
    }

    public Session getSession() {
        return session;
    }

    public void updateInput(InputPacket packet) {
        // Remove inputs that no longer exist (device disconnected)
        while (this.inputs.size() > packet.devices.size()) {
            this.inputs.remove(this.inputs.size() - 1);
        }

        // Add missing inputs (new device connected)
        while (this.inputs.size() < packet.devices.size()) {
            this.inputs.add(null);
        }

        for (int i = 0; i < packet.devices.size(); i++) {
            InputPacket.DevicePacket device = packet.devices.get(i);

            InputDevice input = inputs.get(i);
            if (device.type.equals("keyboard")) {
                if (input instanceof InputKeyboardDevice) {
                    input.updateInput(device);
                } else {
                    input = new InputKeyboardDevice(device);
                }
            } else if (device.type.equals("controller")) {
                if (input instanceof InputControllerDevice) {
                    input.updateInput(device);
                } else {
                    input = new InputControllerDevice(device);
                }
            } else {
                continue;
            }

            this.inputs.set(i, input);
        }
    }

    public void update() {
    }
}
