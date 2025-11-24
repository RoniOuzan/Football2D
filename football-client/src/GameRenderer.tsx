import { JsonData, Team } from "./types";
import { pitchWidthUnits, pitchHeightUnits } from "./types";

export default class GameRenderer {
  draw(canvas: HTMLCanvasElement, data: JsonData) {
    const ctx = canvas.getContext("2d")!;
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // ------------------------------
    // EVERYTHING BELOW IS 100% YOUR CODE
    // copy–pasted with ZERO LOGIC CHANGE
    // ------------------------------

    const playerRadius = 0.75 * (canvas.height / 64);

    const pitchWidth = canvas.width;
    const pitchHeight = canvas.height;
    const goalDepth = (3 / pitchWidthUnits) * pitchWidth;
    const pitchLeft = goalDepth;
    const pitchRight = pitchWidth - goalDepth;

    ctx.lineWidth = 2;
    ctx.strokeStyle = "white";

    // Pitch
    ctx.fillStyle = "#006400";
    ctx.fillRect(pitchLeft, 0, pitchRight - pitchLeft, pitchHeight);
    ctx.strokeRect(pitchLeft, 0, pitchRight - pitchLeft, pitchHeight);

    ctx.beginPath();
    ctx.moveTo(pitchWidth / 2, 0);
    ctx.lineTo(pitchWidth / 2, pitchHeight);
    ctx.stroke();

    const centerCircleRadius = (10 / pitchHeightUnits) * pitchHeight;
    ctx.beginPath();
    ctx.arc(pitchWidth / 2, pitchHeight / 2, centerCircleRadius, 0, Math.PI * 2);
    ctx.stroke();

    ctx.fillStyle = "white";
    ctx.beginPath();
    ctx.arc(pitchWidth / 2, pitchHeight / 2, 4, 0, Math.PI * 2);
    ctx.fill();

    // Penalty boxes
    const penaltyWidth = (16.5 / pitchWidthUnits) * pitchWidth;
    const penaltyHeight = (40.3 / pitchHeightUnits) * pitchHeight;
    const penaltyYOffset = (pitchHeight - penaltyHeight) / 2;

    ctx.strokeRect(pitchLeft, penaltyYOffset, penaltyWidth, penaltyHeight);
    ctx.strokeRect(pitchRight - penaltyWidth, penaltyYOffset, penaltyWidth, penaltyHeight);

    // 6-yard boxes
    const sixYardWidth = (5.5 / pitchWidthUnits) * pitchWidth;
    const sixYardHeight = (18.3 / pitchHeightUnits) * pitchHeight;
    const sixYardYOffset = (pitchHeight - sixYardHeight) / 2;

    ctx.strokeRect(pitchLeft, sixYardYOffset, sixYardWidth, sixYardHeight);
    ctx.strokeRect(pitchRight - sixYardWidth, sixYardYOffset, sixYardWidth, sixYardHeight);

    // Penalty spots
    const penaltySpotOffset = (11 / pitchWidthUnits) * pitchWidth;
    ctx.beginPath();
    ctx.arc(pitchLeft + penaltySpotOffset, pitchHeight / 2, 3, 0, Math.PI * 2);
    ctx.fill();

    ctx.beginPath();
    ctx.arc(pitchRight - penaltySpotOffset, pitchHeight / 2, 3, 0, Math.PI * 2);
    ctx.fill();

    // Ball
    const ball = convert(canvas, data.ball.position, goalDepth);
    ctx.fillStyle = "white";
    ctx.beginPath();
    ctx.arc(ball.x, ball.y, 5, 0, Math.PI * 2);
    ctx.fill();

    // Players
    drawPlayers(canvas, ctx, data.team1, goalDepth, playerRadius, "red");
    drawPlayers(canvas, ctx, data.team2, goalDepth, playerRadius, "blue");

    // Heatmaps
    drawScores(canvas, ctx, data.team1, goalDepth);

    // Defense line
    const defenseX_units = data.team1.teamStrategy.defenseLine;
    const pxLeft = goalDepth;
    const pxRight = canvas.width - goalDepth;

    const defenseX =
      pxLeft + ((defenseX_units + 50) / pitchWidthUnits) * (pxRight - pxLeft);

    ctx.strokeStyle = "yellow";
    ctx.lineWidth = 2;

    ctx.beginPath();
    ctx.moveTo(defenseX, 0);
    ctx.lineTo(defenseX, canvas.height);
    ctx.stroke();
  }
}

  // --- Helper functions ---
function convert(
  canvas: HTMLCanvasElement,
  position: { x: number; y: number },
  goalDepth: number
) {
  const pitchLeft = goalDepth;
  const pitchRight = canvas.width - goalDepth;

  return {
    x: pitchLeft + ((position.x + 50) / pitchWidthUnits) * (pitchRight - pitchLeft),
    y: ((32 - position.y) / pitchHeightUnits) * canvas.height,
  };
}

function scoreToColor(value: number, min: number, max: number): string {
  if (max === min) return "white";

  const t = (value - min) / (max - min);

  const r = Math.floor(255 * t);
  const g = Math.floor(255 * (1 - Math.abs(t - 0.5) * 2));
  const b = Math.floor(255 * (1 - t));

  return `rgb(${r}, ${g}, ${b})`;
}

function drawPlayers(
  canvas: HTMLCanvasElement,
  ctx: CanvasRenderingContext2D,
  team: Team | undefined,
  goalDepth: number,
  radius: number,
  color: string
) {
  if (!team) return;
  team.players.forEach((p, i) => {
    const pos = convert(canvas, p.position, goalDepth);
    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.arc(pos.x, pos.y, radius, 0, Math.PI * 2);
    ctx.fill();

    if (i === team.teamStrategy.chosenPlayerIndex) {
      ctx.strokeStyle = "yellow";
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(pos.x, pos.y, radius, 0, Math.PI * 2);
      ctx.stroke();
    }
  });
}

function drawScores(
  canvas: HTMLCanvasElement,
  ctx: CanvasRenderingContext2D,
  team: Team | undefined,
  goalDepth: number
) {
  if (!team) return;

  const values = team.teamStrategy.scores.map(s => s.value);
  // const min = Math.min(...values);
  const max = Math.min(Math.max(...values), 50);

  team.teamStrategy.scores.forEach((entry) => {
    const pos = convert(canvas, entry.key, goalDepth);
    const color = scoreToColor(entry.value, -120, max);

    ctx.fillStyle = color;
    ctx.globalAlpha = 0.3;
    const sizeX = (canvas.width - 2 * goalDepth) / 40;
    const sizeY = canvas.height / 40;

    ctx.fillRect(pos.x - sizeX / 2, pos.y - sizeY / 2, sizeX, sizeY);
    ctx.globalAlpha = 1.0;
  });
}
