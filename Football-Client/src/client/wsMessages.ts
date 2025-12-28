import type { GameData } from "./jsons/gameTypes";
import type { LobbyData } from "./jsons/lobbyTypes";

export type ServerMessage = LobbyMessage | GameMessage | ErrorMessage;

export interface LobbyMessage {
  type: "lobby";
  data: LobbyData;
}

export interface GameMessage {
  type: "game";
  data: GameData;
}

export interface ErrorMessage {
  type: "error";
  data: {
    message: string;
  };
}
