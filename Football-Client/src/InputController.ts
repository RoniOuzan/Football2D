import Client from "./Client";
import { FPS } from "./game/Game";

export interface DeviceInput {
  type: "keyboard" | "controller" | "mobile";
  buttons: string[];
  axes?: Axes;
}

type MobileJoystick = { x: number; y: number };

interface Axes {
  leftX: number;
  leftY: number;
  rightX: number;
  rightY: number;
  LT: number;
  RT: number;
}

export default class InputController {
  private kbKeys = new Set<string>();
  private client: Client;
  private mobileAxes: MobileJoystick = { x: 0, y: 0 };
  private mobileButtons = new Set<string>();
  private isMobile: boolean;

  constructor(client: Client) {
    this.client = client;
    this.isMobile = false;

    window.addEventListener("keydown", (e) =>
      this.kbKeys.add(e.key.toLowerCase())
    );
    window.addEventListener("keyup", (e) =>
      this.kbKeys.delete(e.key.toLowerCase())
    );

    this.sendAllInputs();
    setInterval(() => this.sendAllInputs(), 1000 / FPS);
  }

  setMobileJoystick(x: number, y: number) {
    this.mobileAxes = { x, y };
  }

  setMobileButton(button: string, pressed: boolean) {
    if (pressed) 
      this.mobileButtons.add(button);
    else 
      this.mobileButtons.delete(button);
  }

  setIsMobile(isMobile: boolean) {
    this.isMobile = isMobile;
  }

  private sendAllInputs() {
    const devices: DeviceInput[] = [];

    if (this.isMobile) {
      devices.push({
        type: "mobile",
        buttons: Array.from(this.mobileButtons),
        axes: {
          leftX: this.mobileAxes.x,
          leftY: this.mobileAxes.y,
          rightX: 0,
          rightY: 0,
          LT: 0,
          RT: 0,
        },
      });
    } else {
      devices.push({
        type: "keyboard",
        buttons: Array.from(this.kbKeys),
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
    const axes: Axes = {
      leftX: 0,
      leftY: 0,
      rightX: 0,
      rightY: 0,
      LT: 0,
      RT: 0,
    };

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
      if (i === 6) axes.LT = b.value;
      else if (i === 7) axes.RT = b.value;

      if (b.pressed) {
        const button = isPS ? MAP_PS[i] : MAP_X[i];
        if (button) inputs.add(button);
      }
    });

    axes.leftX = gp.axes[0] || 0;
    axes.leftY = gp.axes[1] || 0;
    axes.rightX = gp.axes[2] || 0;
    axes.rightY = gp.axes[3] || 0;

    return { type: "controller", buttons: Array.from(inputs), axes };
  }
}
