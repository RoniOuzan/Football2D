package com.football.client;

import org.eclipse.jetty.websocket.api.Session;

public class Client {
    private transient final Session session;
    private transient final ClientInput input = new ClientInput();

    public Client(Session session) {
        this.session = session;
    }

    public ClientInput getInput() {
        return this.input;
    }

    public Session getSession() {
        return session;
    }

    public void setInput(InputValues input) {
        this.input.updateInput(input);
    }

    public void update() {
        this.input.updateInput();
    }
}
