package com.football.client.keybinds;

import com.football.game.strategy.TeamStrategy;
import com.football.game.players.Player;

import java.util.Set;

public interface KeybindAction {
    void justPressed(TeamStrategy teamStrategy, Player player);

    void holding(TeamStrategy teamStrategy, Player player);

    void justReleased(TeamStrategy teamStrategy, Player player, Set<Keybind> kickTypes, double holdTime);

    default double computeHoldTime(double holdTime, double pow, double min, double max) {
        // 0–0.7 sec → 0–1
        double t = Math.min(holdTime / 0.7, 1.0);
        // FIFA-style curve: slow early, fast late
        t = Math.pow(t, pow);

        // FIFA pass range: short 4m → long 32m
        return min + t * (max - min);
    }
}
