import type { Translation3d, Translation2d, Rotation2d } from "../../types";

export interface GameData {
  uuid: string;
  ball: Ball;
  team1: Team;
  team2: Team;
  score1: number;
  score2: number;
  client: number;
}

export function getClientsTeam(data: GameData): Team {
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
