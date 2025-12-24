export const pitchWidth = 100;
export const pitchHeight = 64;
export const maxX = pitchWidth / 2;
export const maxY = pitchHeight / 2;

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
  client: number;
}

export function getClientsTeam(data: JsonData): Team {
  if (data.client == 1) return data.team1;
  if (data.client == 2) return data.team2;
  return data.team1;
}

export interface Ball {
  position: Translation3d;
  velocity: Translation3d;
}

export interface Team {
  players: Player[];
  teamStrategy: TeamStrategy;
}

export interface Player {
  position: Translation2d;
  direction: Rotation2d;
  velocity: Translation2d;
}

export interface Rotation2d {
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
  cameraManager: CameraManager;
}

export interface CameraManager {
  position: Camera;
  positionType: "BROADCAST" | "THIRD_PERSON";
}

export interface Camera {
  translation: Translation3d;
  pitch: Rotation2d;
  yaw: Rotation2d;
  fov: Rotation2d;
}
