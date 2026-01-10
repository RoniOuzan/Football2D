package com.football.client;

import com.football.client.inputs.AllInputDevices;
import com.football.client.inputs.InputDevice;
import com.football.client.inputs.InputHandler;
import com.football.client.inputs.devices.InputControllerDevice;
import com.football.client.inputs.devices.InputKeyboardDevice;
import com.football.client.inputs.devices.InputMobileDevice;
import com.football.client.inputs.devices.InputNullDevice;
import com.football.client.json.InputPacket;
import com.football.client.messages.Message;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Client {
    private transient final Session session;

    private transient final List<InputDevice> inputs;
    private transient final AllInputDevices allInputDevices;

    public Client(Session session) {
        this.session = session;

        this.inputs = new ArrayList<>();
        this.allInputDevices = new AllInputDevices(this.inputs);
    }

    public InputHandler getInput(int slot) {
        if (slot == -1)
            return this.allInputDevices;
        if (slot >= this.inputs.size())
            return new InputNullDevice();
        return this.inputs.get(slot);
    }

    public int getAmountOfInputs() {
        return this.inputs.size();
    }

    public void sendMessage(Message message) {
        try {
            this.session.getRemote().sendString(message.getJson());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(String type, Object data) {
        sendMessage(new Message(type, data));
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
            switch (device.type) {
                case "keyboard" -> {
                    if (input instanceof InputKeyboardDevice) {
                        input.updateInput(device);
                    } else {
                        input = new InputKeyboardDevice(device);
                    }
                }
                case "controller" -> {
                    if (input instanceof InputControllerDevice) {
                        input.updateInput(device);
                    } else {
                        input = new InputControllerDevice(device);
                    }
                }
                case "mobile" -> {
                    if (input instanceof InputMobileDevice) {
                        input.updateInput(device);
                    } else {
                        input = new InputMobileDevice(device);
                    }
                }
                default -> {
                    continue;
                }
            }

            this.inputs.set(i, input);
        }
    }

    public void update() {
    }
}
