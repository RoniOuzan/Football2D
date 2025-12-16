package com.football.game.team;

public enum CameraPosition {
    BROADCAST,
    THIRD_PERSON,
    ;

    public CameraPosition getOther() {
        if (this == BROADCAST)
            return THIRD_PERSON;
        return BROADCAST;
    }
}
