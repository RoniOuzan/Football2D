package com.football.client;

import com.football.game.Element;
import com.football.game.Team;
import org.eclipse.jetty.websocket.api.Session;

import java.util.HashSet;
import java.util.Set;

public class Client implements Element {
    private transient final Session session;

    private transient ClientInput input = new ClientInput();
    private transient Set<String> keys = new HashSet<>();

    private Team team = null;

    public Client(Session session) {
        this.session = session;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public ClientInput getInput() {
        return this.input;
    }

    public Team getTeam() {
        return team;
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

    @Override
    public void update() {
//        System.out.println(this.input.getInput());
        if (this.team != null) {
            this.team.update();
        }

        this.input.updateInput();
    }
}
