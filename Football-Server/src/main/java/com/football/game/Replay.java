package com.football.game;

import java.util.ArrayList;
import java.util.List;

import com.football.GameManager;
import com.football.client.Client;
import com.football.client.messages.Message;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class Replay {

    private static final int MAX_FRAMES = (int) (GameManager.FPS * 5); // 5 seconds

    private boolean active;
    private final List<JsonObject> frames;

    public Replay() {
        this.active = false;
        this.frames = new ArrayList<>();
    }

    public void start(List<Client> allClients) {
        this.active = true;

        JsonObject replayJson = new JsonObject();
        replayJson.add("frames", this.toJsonArray());

        for (Client client : allClients) {
            client.sendMessage(new Message("replay", replayJson));
        }
    }

    public void addFrame(JsonObject jsonObject) {
        this.frames.add(jsonObject);

        if (this.frames.size() > MAX_FRAMES) {
            int removeCount = this.frames.size() - MAX_FRAMES;
            for (int i = 0; i < removeCount; i++) {
                this.frames.remove(0);
            }
        }
    }

    public boolean isActive() {
        return this.active;
    }

    public void stop() {
        this.active = false;
        this.frames.clear();
    }

    public JsonArray toJsonArray() {
        JsonArray arr = new JsonArray();
        for (JsonObject frame : this.frames) {
            arr.add(frame);
        }
        return arr;
    }
}
