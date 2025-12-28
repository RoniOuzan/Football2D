import InputController from "../InputController";
import type { ServerMessage } from "./wsMessages";

export default class Client {
  private socket?: WebSocket;

  private onMessage: (msg: ServerMessage) => void;
  private input: InputController;

  constructor(onMessage: (msg: ServerMessage) => void) {
    this.onMessage = onMessage;
    this.input = new InputController(this);

    this.connect();
  }

  public getInput() {
    return this.input;
  }

  sendJSON(type: string, data: any) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return;

    this.socket.send(JSON.stringify({ type, data }));
  }

  connect() {
    const host = window.location.hostname;
    this.socket = new WebSocket(`wss://${host}:9090/game`);

    this.socket.onopen = () => {
      console.log("WS connected");
    };

    this.socket.onmessage = (e) => {
      if (!e.data) return;
      const msg = JSON.parse(e.data) as ServerMessage;
      this.onMessage(msg);
    };

    this.socket.onerror = (e) => {
      console.error("WS error", e);
    };

    this.socket.onclose = (e) => {
      console.log("WS closed", e.code, e.reason);
    };
  }

  createGame() {
    // TODO:
  }

  joinGame(id: string) {
    // TODO:
  }
}
