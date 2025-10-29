package com.football;

import com.football.game.Ball;
import com.football.game.Element;
import com.football.util.json.JsonUtil;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private final List<Element> elements;

    public Game() {
        this.elements = new ArrayList<>();

        this.reset();
    }

    public List<Element> getElements() {
        return this.elements;
    }

    public void reset() {
        this.elements.add(new Ball());
//        this.elements.add(new Player());
    }

    public void update() {
        for (Element element : this.elements) {
            element.update();
        }
    }

    public String toJson() {
        return JsonUtil.toJson(this.elements.get(0));
    }
}
