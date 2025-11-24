package com.football.client;

import org.eclipse.jetty.websocket.api.Session;

import java.util.ArrayList;
import java.util.List;

public class Client {
    private transient final Session session;
    private transient final List<ClientInput> inputs = new ArrayList<>();

    public Client(Session session) {
        this.session = session;
    }

    public List<ClientInput> getInputs() {
        return inputs;
    }

    public ClientInput getInput(int slot) {
        if (slot >= this.inputs.size()) return null;
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

            ClientInput input = inputs.get(i);
            if (device.type.equals("keyboard")) {
                if (input instanceof InputKeyboard) {
                    input.updateInput(device);
                } else {
                    input = new InputKeyboard(device);
                }
            } else if (device.type.equals("controller")) {
                if (input instanceof InputController) {
                    input.updateInput(device);
                } else {
                    input = new InputController(device);
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
