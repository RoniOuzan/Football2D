import type { AlertData } from "./jsons/alertTypes";
import type { CountdownData } from "./jsons/countdownTypes";
import type { GameData, ReplayData } from "./jsons/gameTypes";
import type { LobbyData } from "./jsons/lobbyTypes";

export type ServerMessage = LobbyMessage | GameMessage | ReplayMessage | AlertMessage | CountdownMessage;

export interface LobbyMessage {
  type: "lobby";
  data: LobbyData;
}

export interface GameMessage {
  type: "game";
  data: GameData;
}

export interface ReplayMessage {
  type: "replay";
  data: ReplayData;
}

export interface AlertMessage {
  type: "alert";
  data: AlertData;
}

export interface CountdownMessage {
  type: "countdown";
  data: CountdownData;
}
