import type { GameData } from "./jsons/gameTypes";
import type { LobbyData } from "./jsons/lobbyTypes";

export type ServerMessage = LobbyMessage | GameMessage | AlertMessage;

export interface LobbyMessage {
  type: "lobby";
  data: LobbyData;
}

export interface GameMessage {
  type: "game";
  data: GameData;
}

export interface AlertMessage {
  type: "alert";
  data: {
    message: string;
  };
}
