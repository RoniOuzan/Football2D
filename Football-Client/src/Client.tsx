import type { JsonData } from "./types";

export default class Client {
  private socket?: WebSocket;
  private setData: (data: JsonData) => void;

  constructor(setData: (data: JsonData) => void) {
    this.setData = setData;
  }

  connect() {
    const host = window.location.hostname;
    this.socket = new WebSocket(`wss://${host}:9090/game`);

    this.socket.onopen = () => {
      console.log("WS connected");
    };

    this.socket.onmessage = (ev) => {
      if (!ev.data) return;
      this.setData(JSON.parse(ev.data));
    };

    this.socket.onerror = (e) => {
      console.error("WS error", e);
    };

    this.socket.onclose = (e) => {
      console.log("WS closed", e.code, e.reason);
    };
  }

  sendJSON(type: string, data: any) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return;
    this.socket.send(JSON.stringify({ type, data }));
  }
}
