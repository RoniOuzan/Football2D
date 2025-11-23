import Client from "./Client";
import { FPS } from "./Game";

export interface InputPacket {
  keys: String[],
  buttons: String[],
  axes: Axes,
}

interface Axes {
  leftX: number, leftY: number,
  rightX: number, rightY: number,
  LT: number, RT: number,
}

export default class InputController {
  private kbKeys = new Set<string>();
  private buttons = new Set<string>();

  private axes: Axes = {
    leftX: 0, leftY: 0,
    rightX: 0, rightY: 0,
    LT: 0, RT: 0
  };

  private client: Client;

  constructor(client: Client) {
    this.client = client;

    window.addEventListener("keydown", e => this.kbKeys.add(e.key));
    window.addEventListener("keyup", e => this.kbKeys.delete(e.key));

    setInterval(() => {
      this.readController();
      this.client.sendInput({
        keys: Array.from(this.kbKeys),
        buttons: Array.from(this.buttons),
        axes: this.axes
      });
    }, 1000 / FPS);
  }

  private readController() {
    const gp = navigator.getGamepads()[0];
    if (!gp) return;

    this.buttons.clear();

    const name = (gp.id + "").toLowerCase();

    if (name.includes("xbox")) this.mapXbox(gp);
    else if (name.includes("dualshock") || name.includes("wireless controller") || name.includes("ps"))
      this.mapPlayStation(gp);
    else this.mapGeneric(gp);
  }

  /** ------------------------ XBOX ------------------------ **/
  private mapXbox(gp: Gamepad) {
    const m = [
      "A","B","X","Y",
      "LB","RB","LT","RT",
      "BACK","START","LS","RS",
      "UP","DOWN","LEFT","RIGHT"
    ];

    gp.buttons.forEach((b, i) => {
      if (i === 6) this.axes.LT = b.value;
      else if (i === 7) this.axes.RT = b.value;
      else if (b.pressed) this.buttons.add(m[i]);
    });

    this.axes.leftX  = gp.axes[0] || 0;
    this.axes.leftY  = gp.axes[1] || 0;
    this.axes.rightX = gp.axes[2] || 0;
    this.axes.rightY = gp.axes[3] || 0;
  }

  /** ------------------------ PLAYSTATION ------------------------ **/
  private mapPlayStation(gp: Gamepad) {
    // PS button index → Xbox button name
    const PS_TO_XBOX: Record<number, string> = {
      0: "A", // Cross
      1: "B", // Circle
      2: "X", // Square
      3: "Y", // Triangle
      4: "LB",
      5: "RB",
      8: "BACK",
      9: "START",
      10: "LS",
      11: "RS",
      12: "UP",
      13: "DOWN",
      14: "LEFT",
      15: "RIGHT"
    };

    gp.buttons.forEach((b, i) => {
      if (i === 6) this.axes.LT = b.value; // L2
      else if (i === 7) this.axes.RT = b.value; // R2
      else if (b.pressed && PS_TO_XBOX[i]) this.buttons.add(PS_TO_XBOX[i]);
    });

    this.axes.leftX = gp.axes[0] || 0;
    this.axes.leftY = gp.axes[1] || 0;
    this.axes.rightX = gp.axes[2] || 0;
    this.axes.rightY = gp.axes[3] || 0;
  }

  /** ------------------------ GENERIC ------------------------ **/
  private mapGeneric(gp: Gamepad) {
    const m = [
      "A","B","X","Y",
      "LB","RB","LT","RT",
      "BACK","START","LS","RS",
      "UP","DOWN","LEFT","RIGHT"
    ];

    gp.buttons.forEach((b, i) => {
      if (i === 6) this.axes.LT = b.value;
      else if (i === 7) this.axes.RT = b.value;
      else if (b.pressed && m[i]) this.buttons.add(m[i]);
    });

    this.axes.leftX = gp.axes[0] || 0;
    this.axes.leftY = gp.axes[1] || 0;
    this.axes.rightX = gp.axes[2] || 0;
    this.axes.rightY = gp.axes[3] || 0;
  }

  /** Vibrate the controller (works on Xbox/PS/Generic if supported) */
  // public vibrate(strength: number = 1, duration: number = 200) {
  //   const gp = navigator.getGamepads()[0];
  //   if (!gp || !gp.vibrationActuator) return;

  //   gp.vibrationActuator.playEffect("dual-rumble", {
  //     startDelay: 0,
  //     duration,
  //     strongMagnitude: strength,
  //     weakMagnitude: strength
  //   }).catch(() => {});
  // }
}
