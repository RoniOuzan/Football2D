import type { GameData } from "./gameTypes";

export interface LobbyData {
  waitingGames: WaitingGame[];
  games: GameData[];
}

export interface WaitingGame {
  uuid: string;
}
