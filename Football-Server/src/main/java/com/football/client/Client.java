package com.football.client;

import com.football.game.Team;
import org.eclipse.jetty.websocket.api.Session;

import java.util.HashSet;
import java.util.Set;

public class Client {
    private transient final Session session;

    private final transient ClientInput input = new ClientInput();
    private transient Set<String> keys = new HashSet<>();

    public Client(Session session) {
        this.session = session;
    }

    public ClientInput getInput() {
        return this.input;
    }

    public Session getSession() {
        return session;
    }

    public Set<String> getKeys() {
        return keys;
    }

    public void setInput(InputValues input) {
        this.input.updateInput(input);
        this.keys = input.keys;
    }

    public void update() {
        this.input.updateInput();
    }
}
