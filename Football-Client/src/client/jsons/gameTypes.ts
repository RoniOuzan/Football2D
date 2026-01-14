import type { Translation3d, Translation2d, Rotation2d } from "../../types";

export type Phase = "FIRST_HALF" | "SECOND_HALF" | "EXTRA_TIME" | "FINISH";

export interface ReplayData {
  active: boolean;
  scoredSide: number;
  frames: GameData[];
}

export interface GameData {
  uuid: string;
  ball: Ball;
  team1: Team;
  team2: Team;
  score1: number;
  score2: number;
  matchTime: number;
  phase: Phase;
  addedTime: number;
  state: string;
  spectators: Spectator[];
  client: number;

  replay: ReplayData;
}

export function getClientsTeam(data: GameData): Team | null {
  if (data.client == 1) return data.team1;
  if (data.client == 2) return data.team2;
  return null;
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

export interface Spectator {
  cameraManager: CameraManager;
}