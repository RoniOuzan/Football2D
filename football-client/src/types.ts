export const pitchWidthUnits = 100;
export const pitchHeightUnits = 64;
export const maxX = pitchWidthUnits / 2;
export const maxY = pitchHeightUnits / 2;

export interface Translation2d {
  x: number;
  y: number;
}

export interface Translation3d {
  x: number;
  y: number;
  z: number;
}

export interface JsonData {
  ball: Ball;
  team1: Team;
  team2: Team;
  score1: number;
  score2: number;
}

export interface Ball {
  position: Translation2d;
  velocity: Translation2d;
}

export interface Team {
  players: Player[];
  teamStrategy: TeamStrategy;
}

export interface Player {
  position: Translation2d;
  direction: Direction;
  velocity: Translation2d;
}

export interface Direction {
  value: number;
  cos: number;
  sin: number;
}

export interface TeamStrategy {
  scores: {
    key: Translation2d;
    value: number;
  }[];
  chosenPlayerIndex: number;
}
