import { DeviceInput } from "./InputController";
import { JsonData } from "./types";

export default class Client {
  private socket?: WebSocket;
  private setData: (data: JsonData) => void;

  constructor(setData: (data: JsonData) => void) {
    this.setData = setData;
  }

  connect() {
    this.socket = new WebSocket("ws://192.168.129.62:9090/game");

    this.socket.onmessage = (ev) => {
      if (!ev.data) return;
      this.setData(JSON.parse(ev.data));
    };
  }

  sendJSON(data: any) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return;
    this.socket.send(JSON.stringify({ type: "input", data }));
  }
}
