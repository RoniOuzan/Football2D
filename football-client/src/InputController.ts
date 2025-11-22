// InputController.ts
import Client from "./Client";

export interface InputPacket {
  keyboard: string[];
  controller: string[];
  axes: number[];
}

export default class InputController {
  private keyboardKeys = new Set<string>();
  private controllerKeys = new Set<string>();
  private axes: number[] = [];
  private client: Client;

  constructor(client: Client) {
    this.client = client;

    // Keyboard
    window.addEventListener("keydown", (e) => this.keyboardKeys.add(e.key));
    window.addEventListener("keyup", (e) => this.keyboardKeys.delete(e.key));

    setInterval(() => {
      this.pollGamepads();

      const packet: InputPacket = {
        keyboard: Array.from(this.keyboardKeys),
        controller: Array.from(this.controllerKeys),
        axes: [...this.axes],
      };

      this.client.sendInput(packet);
    }, 1000 / 30);
  }

  private pollGamepads() {
    const gps = navigator.getGamepads ? navigator.getGamepads() : [];
    this.axes = [];
    this.controllerKeys.clear();

    for (const gp of gps) {
      if (!gp) continue;

      // Buttons
      gp.buttons.forEach((b, i) => {
        if (b.pressed) this.controllerKeys.add(`button${i}`);
      });

      // Axes
      this.axes.push(...gp.axes.map((a) => parseFloat(a.toFixed(2))));
    }
  }
}
