import { isMobile } from "./App";
import Client from "./client/Client";
import { FPS } from "./game/Game";
import type { Translation2d } from "./types";

export interface DeviceInput {
  type: "keyboard" | "controller" | "mobile";
  buttons: string[];
  axes: number[];
  click?: Translation2d | null;
}

export default class InputController {
  private kbKeys = new Set<string>();
  private client: Client;
  private axes: number[] = [0, 0];
  private mobileButtons = new Set<string>();
  private click: Translation2d | null = null;

  constructor(client: Client) {
    this.client = client;

    window.addEventListener("keydown", (e) =>
      this.kbKeys.add(e.key.toLowerCase())
    );
    window.addEventListener("keyup", (e) =>
      this.kbKeys.delete(e.key.toLowerCase())
    );

    this.sendAllInputs();
    setInterval(() => this.sendAllInputs(), 1000 / FPS);
  }

  public setJoystick(x: number, y: number) {
    this.axes = [x, y];
  }

  public setClick(click: Translation2d | null) {
    this.click = click;
  }

  public getAxes() {
    return this.axes;
  }

  public setMobileButton(button: string, pressed: boolean) {
    if (pressed) this.mobileButtons.add(button);
    else this.mobileButtons.delete(button);
  }

  private sendAllInputs() {
    const devices: DeviceInput[] = [];

    if (isMobile) {
      devices.push({
        type: "mobile",
        buttons: Array.from(this.mobileButtons),
        axes: this.axes,
        click: this.click,
      });
    } else {
      devices.push({
        type: "keyboard",
        buttons: Array.from(this.kbKeys),
        axes: this.axes,
        click: this.click,
      });
    }

    // controllers
    for (const gp of navigator.getGamepads()) {
      if (!gp) continue;
      if (
        !gp.id.includes("Controller") &&
        !gp.id.includes("Gamepad") &&
        !gp.id.includes("Wireless")
      )
        continue;
      devices.push(this.mapController(gp));
    }

    this.client.sendJSON("input", { devices: devices });
  }

  private mapController(gp: Gamepad): DeviceInput {
    const inputs = new Set<string>();
    const axes: number[] = [0, 0, 0, 0, 0, 0];

    const name = gp.id.toLowerCase();
    const isPS =
      name.includes("dualshock") ||
      name.includes("wireless") ||
      name.includes("ps");

    const MAP_X = [
      "A",
      "B",
      "X",
      "Y",
      "LB",
      "RB",
      "LT",
      "RT",
      "BACK",
      "START",
      "LS",
      "RS",
      "UP",
      "DOWN",
      "LEFT",
      "RIGHT",
    ];
    const MAP_PS: Record<number, string> = {
      0: "A",
      1: "B",
      2: "X",
      3: "Y",
      4: "LB",
      5: "RB",
      6: "LT",
      7: "RT",
      8: "BACK",
      9: "START",
      10: "LS",
      11: "RS",
      12: "UP",
      13: "DOWN",
      14: "LEFT",
      15: "RIGHT",
    };

    gp.buttons.forEach((b, i) => {
      if (i === 6) axes[4] = b.value;
      else if (i === 7) axes[5] = b.value;

      if (b.pressed) {
        const button = isPS ? MAP_PS[i] : MAP_X[i];
        if (button) inputs.add(button);
      }
    });

    axes[0] = gp.axes[0] || 0;
    axes[1] = gp.axes[1] || 0;
    axes[2] = gp.axes[2] || 0;
    axes[3] = gp.axes[3] || 0;

    return { type: "controller", buttons: Array.from(inputs), axes };
  }
}
