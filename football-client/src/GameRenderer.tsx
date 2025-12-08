import React, { useRef, useEffect, useState } from "react";
import { JsonData, maxX, Player, Team, Translation2d } from "./types";
import { pitchWidthUnits, pitchHeightUnits } from "./types";

const HEIGHT_SCALE = 0.45;      // Flatten vertical scale for FIFA perspective
const TOP_SCALE = 0.25;         // Top edge scaling (strong perspective)
const BOTTOM_SCALE = 1.0;       // Bottom edge scaling (full width)
const ZOOM = 1.7;               // Medium zoom level
const TILT_Y = 0;               // Camera vertical tilt

interface Camera {
  x: number;
  y: number;
}

interface GameRendererProps {
  data: JsonData;
}

// Convert meters to X pixels relative to the pitch
function toPixelX(
  canvas: HTMLCanvasElement,
  goalDepth: number,
  xMeters: number
): number {
  const pitchLeft = goalDepth;
  const pitchRight = canvas.width - goalDepth;
  return pitchLeft + ((xMeters + pitchWidthUnits / 2) / pitchWidthUnits) * (pitchRight - pitchLeft);
}

// Convert meters to Y pixels relative to the pitch
function toPixelY(
  canvas: HTMLCanvasElement,
  yMeters: number
): number {
  return ((pitchHeightUnits / 2 - yMeters) / pitchHeightUnits) * canvas.height;
}

// Convert meters to radius in pixels
function toPixelRadius(
  canvas: HTMLCanvasElement,
  goalDepth: number,
  radiusMeters: number
): number {
  return radiusMeters * ((canvas.width - 2 * goalDepth) / pitchWidthUnits);
}

// Convert a world position in meters to screen position considering perspective and camera
function worldToScreen(
  canvas: HTMLCanvasElement,
  goalDepth: number,
  position: Translation2d,
  camera: Camera
): Translation2d {
  const pixelX = toPixelX(canvas, goalDepth, position.x);
  const pixelY = toPixelY(canvas, position.y);
  return perspectivePoint(pixelX, pixelY, canvas, camera);
}

const GameRenderer3D: React.FC<GameRendererProps> = ({ data }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [camera, setCamera] = useState<Camera>({ x: 0, y: 0 });

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const goalDepth = (3 / pitchWidthUnits) * canvas.width;
    const fieldWidthPX = canvas.width - 2 * goalDepth;

    // Smooth camera follow: limit ball movement and scale
    const effectiveBallX = Math.max(Math.min(data.ball.position.x, 50), -50) * 0.5;
    const cameraX = (effectiveBallX / pitchWidthUnits / 2) * fieldWidthPX;

    const effectiveBallY = Math.max(Math.min(data.ball.position.y, 16), -16) * 0.5;
    const cameraY = (effectiveBallY / pitchHeightUnits / 2) * canvas.height;

    setCamera({ x: cameraX, y: cameraY });

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    drawPitch(ctx, canvas, goalDepth, camera);

    // Halfway line
    const halfTop = perspectivePoint(canvas.width / 2, 0, canvas, camera);
    const halfBottom = perspectivePoint(canvas.width / 2, canvas.height, canvas, camera);
    ctx.beginPath();
    ctx.moveTo(halfTop.x, halfTop.y);
    ctx.lineTo(halfBottom.x, halfBottom.y);
    ctx.stroke();

    // Center circle
    drawCircleWorld(ctx, canvas, { x: 0, y: 0 }, 9.15, goalDepth, "white", camera, false);
    drawCircleWorld(ctx, canvas, { x: 0, y: 0}, 0.25, goalDepth, "white", camera, true);

    // Penalty boxes
    drawRectWorld(ctx, canvas, goalDepth, 16.5, 40.3, camera);
    drawRectWorld(ctx, canvas, goalDepth, 16.5, 40.3, camera, true);

    // Six-yard boxes
    drawRectWorld(ctx, canvas, goalDepth, 5.5, 18.3, camera);
    drawRectWorld(ctx, canvas, goalDepth, 5.5, 18.3, camera, true);

    // Penalty spots
    drawCircleWorld(ctx, canvas, { x: maxX - 11, y: 0}, 0.25, goalDepth, "white", camera, true);
    drawCircleWorld(ctx, canvas, { x: -(maxX - 11), y: 0}, 0.25, goalDepth, "white", camera, true);

    // Heatmap
    // drawHeatmap(ctx, canvas, data.team1, goalDepth, camera);

    // Goals
    drawGoals3D(ctx, canvas, goalDepth, camera);

    const draws: { obj: Player | 'ball', depth: number, draw: () => void }[] = [];

    // Ball
    draws.push({
      obj: 'ball',
      depth: getScale(canvas, data.ball.position.y),
      draw: () => drawCircle(ctx, canvas, data.ball.position, 0.35, goalDepth, "white", camera, true),
    });

    // Players
    data.team1.players.forEach((player, i) => {
      draws.push({
        obj: player,
        depth: getScale(canvas, player.position.y),
        draw: () => drawPlayer(ctx, canvas, player, i, data.team1.teamStrategy.chosenPlayerIndex, goalDepth, "red", camera),
        // draw: () => drawCircleWorld(ctx, canvas, player.position, 0.75, goalDepth, "red", camera, true)
      });
    });
    data.team2.players.forEach((player, i) => {
      draws.push({
        obj: player,
        depth: getScale(canvas, player.position.y),
        draw: () => drawPlayer(ctx, canvas, player, i, data.team2.teamStrategy.chosenPlayerIndex, goalDepth, "blue", camera),
        // draw: () => drawCircleWorld(ctx, canvas, player.position, 0.75, goalDepth, "blue", camera, true)
      });
    });

    draws.sort((a, b) => b.depth - a.depth);
    draws.forEach(d => d.draw());

  }, [data, camera]);

  return <canvas ref={canvasRef} style={{ backgroundColor: "#006400" }} />;
};

// -----------------------
// Perspective helpers
// -----------------------
function perspectivePoint(
  x: number,
  y: number,
  canvas: HTMLCanvasElement,
  camera: Camera
): Translation2d {
  let newY = y * HEIGHT_SCALE + (1 - HEIGHT_SCALE) * canvas.height / 2;
  newY += TILT_Y;
  const scale = getScale(canvas, y);
  const centerX = canvas.width / 2;
  const centerY = canvas.height / 2;
  const dx = (x - centerX) * scale * ZOOM;
  const dy = (newY - centerY) * ZOOM;
  return {
    x: centerX + dx - camera.x * ZOOM,
    y: centerY + dy + camera.y * ZOOM
  };
}

function getScale(
  canvas: HTMLCanvasElement,
  y: number
): number {
  y = y * HEIGHT_SCALE + (1 - HEIGHT_SCALE) * canvas.height / 2 + TILT_Y;
  const t = y / canvas.height;
  return TOP_SCALE + (BOTTOM_SCALE - TOP_SCALE) * t;
}

// -----------------------
// Drawing helpers
// -----------------------
function drawCircleWorld(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  position: Translation2d,
  radiusMeters: number,
  goalDepth: number,
  color: string,
  camera: Camera,
  filled: boolean,
  startAngle: number = 0,
  endAngle: number = Math.PI * 2,
) {
  const pixelRadius = toPixelRadius(canvas, goalDepth, radiusMeters);
  const screen = worldToScreen(canvas, goalDepth, position, camera);
  // Perspective scaling: farther objects are flatter, closer more circular
  const scale = getScale(canvas, toPixelY(canvas, position.y));
  const radiusX = pixelRadius * ZOOM * scale;
  const radiusY = radiusX * 0.6; // more flattening for distant

  ctx.beginPath();
  ctx.ellipse(screen.x, screen.y, radiusX, radiusY, 0, startAngle, endAngle);
  ctx.strokeStyle = color;
  ctx.fillStyle = color;
  filled ? ctx.fill() : ctx.stroke();
}

function drawCircle(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  position: Translation2d,
  radiusMeters: number,
  goalDepth: number,
  color: string,
  camera: Camera,
  filled: boolean
) {
  const pixelRadius = toPixelRadius(canvas, goalDepth, radiusMeters);
  const screen = worldToScreen(canvas, goalDepth, position, camera);
  // Perspective scaling: farther objects are flatter, closer more circular
  const scale = getScale(canvas, toPixelY(canvas, position.y));
  const radius = pixelRadius * ZOOM * scale;

  ctx.beginPath();
  ctx.arc(screen.x, screen.y, radius, 0, Math.PI * 2);
  ctx.strokeStyle = color;
  ctx.fillStyle = color;
  filled ? ctx.fill() : ctx.stroke();
}

const playersLastVelocity = new Map<Player, Translation2d>();

function drawPlayer(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  player: Player,
  index: number,
  chosenPlayer: number,
  goalDepth: number,
  color: string,
  camera: Camera
) {
  const position = player.position;
  const velocity = player.velocity;
  const screen = worldToScreen(canvas, goalDepth, position, camera);

  const speed = Math.hypot(velocity.x, velocity.y);

  // Perspective scaling
  const scale = getScale(canvas, toPixelY(canvas, position.y)) * ZOOM;

  const bodyHeight = toPixelRadius(canvas, goalDepth, 1.2) * scale;
  const bodyWidth = toPixelRadius(canvas, goalDepth, 1) * scale;
  const radiusFeetY = (bodyWidth / 2) * 0.6;
  const headRadius = toPixelRadius(canvas, goalDepth, 0.3) * scale;

  const x = screen.x;
  const y = screen.y;
  const yOffset = 0.7;

  ctx.save();

  // =====================================================
  // ⭐ IMPROVED RUNNING ANIMATION (stable speed + accel fix)
  // =====================================================

  const vx = velocity.x;
  const vy = velocity.y;

  // Speed + acceleration
  const lastVel = playersLastVelocity.get(player) ?? {x: 0, y: 0};
  const ax = vx - lastVel.x;
  const ay = vy - lastVel.y;
  playersLastVelocity.set(player, velocity);

  // Direction of movement
  const dir = -player.direction.value;

  // ====================================
  // 🧠 REALISTIC STEP FREQUENCY (FIFA-like)
  // ====================================
  const baseStepHz = 1.6;
  const maxStepHz = 3.2;
  const normalizedSpeed = Math.min(speed / 6, 1);
  const stepHz = baseStepHz + (maxStepHz - baseStepHz) * normalizedSpeed;

  // ====================================
  // 🏃‍♂️ RUNNING STABILITY FACTOR
  // prevents huge leg swing during accel/decel
  // ====================================
  const accelMag = Math.hypot(ax, ay);
  const maxAccel = 0.45; // tune based on game physics
  let runningFactor = 1 - Math.min(accelMag / maxAccel, 1);
  runningFactor = Math.pow(runningFactor, 1.6); // smoother falloff

  // ====================================
  // 👣 LEG MOTION
  // ====================================
  const phase = (performance.now() * 0.001 * stepHz) % 1;
  const step = Math.sin(phase * Math.PI * 2);

  const sideSwing = step * (bodyWidth * 0.22) * runningFactor;
  const strideMax = bodyWidth * 0.35;
  const forwardSwing =
    Math.cos(phase * Math.PI * 2) *
    (strideMax * normalizedSpeed * runningFactor);

  // rotate by direction
  const fx = Math.cos(dir) * forwardSwing;
  const fy = Math.sin(dir) * forwardSwing;

  // FOOT POSITIONS (idle, walk, jog, etc)
  const idle = speed < 0.1 || runningFactor < 0.1;

  const leftFootX = idle ? x - bodyWidth * 0.22 : x - sideSwing + fx;
  const leftFootY = idle ? y + radiusFeetY : y + radiusFeetY + fy * 0.25;

  const rightFootX = idle ? x + bodyWidth * 0.22 : x + sideSwing - fx;
  const rightFootY = idle ? y + radiusFeetY : y + radiusFeetY - fy * 0.25;

  // =====================================================
  // 🦵 LEGS (drawn BEFORE body → more FIFA-like)
  // =====================================================

  ctx.fillStyle = color;

  // Left foot
  ctx.beginPath();
  ctx.ellipse(leftFootX, leftFootY, bodyWidth * 0.28, radiusFeetY, 0, 0, Math.PI * 2);
  ctx.fill();

  // Right foot
  ctx.beginPath();
  ctx.ellipse(rightFootX, rightFootY, bodyWidth * 0.28, radiusFeetY, 0, 0, Math.PI * 2);
  ctx.fill();

  // =====================================================
  // 👕 BODY
  // =====================================================

  ctx.fillStyle = color;
  ctx.beginPath();
  ctx.roundRect(
    x - bodyWidth / 2,
    y - bodyHeight * yOffset,
    bodyWidth,
    bodyHeight
  );
  ctx.fill();

  // =====================================================
  // 😀 HEAD
  // =====================================================

  ctx.beginPath();
  ctx.fillStyle = "#ffe0bd";
  ctx.arc(
    x,
    y - bodyHeight * yOffset - headRadius + 2 * scale,
    headRadius,
    0,
    Math.PI * 2
  );
  ctx.fill();

  if (index === chosenPlayer) {
    drawCircleWorld(ctx, canvas, position, 0.8, goalDepth, "yellow", camera, false, 305 * Math.PI / 180, 235 * Math.PI / 180);
  }

  ctx.restore();
}

// function drawPlayer(
//   ctx: CanvasRenderingContext2D,
//   canvas: HTMLCanvasElement,
//   player: Player,
//   goalDepth: number,
//   color: string,
//   camera: Camera
// ) {
//   const position = player.position;
//   const screen = worldToScreen(canvas, goalDepth, position, camera);

//   // Perspective scaling
//   const scale = getScale(canvas, toPixelY(canvas, position.y)) * ZOOM;

//   const bodyHeight = toPixelRadius(canvas, goalDepth, 1.2) * scale;      // player height
//   const bodyWidth = toPixelRadius(canvas, goalDepth, 1) * scale;       // torso width
//   const radiusFeetY = bodyWidth / 2 * 0.6; // more flattening for distant
//   const headRadius = toPixelRadius(canvas, goalDepth, 0.3) * scale;

//   const x = screen.x;
//   const y = screen.y;
//   const yOffset = 0.7;

//   ctx.save();

//   // --- BODY ---
//   ctx.fillStyle = color;
//   ctx.beginPath();
//   ctx.roundRect(
//     x - bodyWidth / 2,
//     y - bodyHeight * yOffset,
//     bodyWidth,
//     bodyHeight
//   );
//   ctx.fill();

//   // --- FEET ---
//   ctx.beginPath();
//   ctx.fillStyle = color;
//   ctx.ellipse(x, y + radiusFeetY, bodyWidth / 2, radiusFeetY, 0, 0, Math.PI);
//   ctx.fill();

//   ctx.beginPath();
//   ctx.fillStyle = color;
//   ctx.ellipse(x, y - bodyHeight * yOffset + 1, bodyWidth / 2, radiusFeetY, 0, Math.PI, Math.PI * 2);
//   ctx.fill();

//   // --- HEAD ---
//   ctx.beginPath();
//   ctx.fillStyle = "#ffe0bd";  // light skin tone; you can change
//   ctx.arc(x, y - bodyHeight * yOffset - headRadius + 2 * scale, headRadius, 0, Math.PI * 2);
//   ctx.fill();

//   ctx.restore();
// }

function drawHeatmap(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  team: Team | undefined,
  goalDepth: number,
  camera: Camera
) {
  if (!team) return;
  const scoreValues = team.teamStrategy.scores.map(score => score.value);
  const maxValue = Math.min(Math.max(...scoreValues), 50);
  ctx.save();
  ctx.globalAlpha = 0.3;
  const sizeX = pitchWidthUnits / 40;
  const sizeY = pitchHeightUnits / 40;
  for (const entry of team.teamStrategy.scores) {
    const color = scoreToColor(entry.value, -120, maxValue);
    fillPoly(ctx, color, [
      worldToScreen(canvas, goalDepth, { x: entry.key.x - sizeX / 2, y: entry.key.y - sizeY / 2 }, camera),
      worldToScreen(canvas, goalDepth, { x: entry.key.x - sizeX / 2, y: entry.key.y + sizeY / 2 }, camera),
      worldToScreen(canvas, goalDepth, { x: entry.key.x + sizeX / 2, y: entry.key.y + sizeY / 2 }, camera),
      worldToScreen(canvas, goalDepth, { x: entry.key.x + sizeX / 2, y: entry.key.y - sizeY / 2 }, camera)
    ]);
  }
  ctx.restore();
}

function fillPoly(
  ctx: CanvasRenderingContext2D,
  color: string,
  points: Translation2d[]
) {
  ctx.fillStyle = color;
  ctx.beginPath();
  ctx.moveTo(points[0].x, points[0].y);
  for (let i = 1; i < points.length; i++) {
    ctx.lineTo(points[i].x, points[i].y);
  }
  ctx.closePath();
  ctx.fill();
}

function drawPitch(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  goalDepth: number,
  camera: Camera
) {
  const topLeft = perspectivePoint(goalDepth, 0, canvas, camera);
  const topRight = perspectivePoint(canvas.width - goalDepth, 0, canvas, camera);
  const bottomLeft = perspectivePoint(goalDepth, canvas.height, canvas, camera);
  const bottomRight = perspectivePoint(canvas.width - goalDepth, canvas.height, canvas, camera);
  ctx.fillStyle = "#008800";
  ctx.strokeStyle = "white";
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.moveTo(topLeft.x, topLeft.y);
  ctx.lineTo(topRight.x, topRight.y);
  ctx.lineTo(bottomRight.x, bottomRight.y);
  ctx.lineTo(bottomLeft.x, bottomLeft.y);
  ctx.closePath();
  ctx.fill();
  ctx.stroke();
}

function drawRectWorld(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  goalDepth: number,
  widthM: number,
  heightM: number,
  camera: Camera,
  mirror = false
) {
  const pxW = (widthM / pitchWidthUnits) * (canvas.width - 2 * goalDepth);
  const pxH = (heightM / pitchHeightUnits) * canvas.height;
  const left = mirror ? canvas.width - goalDepth - pxW : goalDepth;
  const top = (canvas.height - pxH) / 2;
  const TL = perspectivePoint(left, top, canvas, camera);
  const TR = perspectivePoint(left + pxW, top, canvas, camera);
  const BL = perspectivePoint(left, top + pxH, canvas, camera);
  const BR = perspectivePoint(left + pxW, top + pxH, canvas, camera);
  ctx.beginPath();
  ctx.moveTo(TL.x, TL.y);
  ctx.lineTo(TR.x, TR.y);
  ctx.lineTo(BR.x, BR.y);
  ctx.lineTo(BL.x, BL.y);
  ctx.closePath();
  ctx.stroke();
}

function drawGoals3D(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  goalDepth: number,
  camera: Camera
) {
  const goalWidth = 7.32; // meters
  const goalHeight = 2.44; // meters
  const postThickness = 0.2; // meters, make it wider

  const goals = [
    { x: -maxX, mirror: false },   // left goal
    { x: maxX, mirror: true }     // right goal
  ];

  goals.forEach(goal => {
    const frontX = goal.x;
    const backX = goal.mirror ? goal.x + 3 : goal.x - 3;
    const topY = goalWidth / 2;
    const bottomY = -goalWidth / 2;

    // Front posts
    const LFTop = worldToScreen(canvas, goalDepth, { x: frontX, y: topY }, camera);
    const LFBottom = worldToScreen(canvas, goalDepth, { x: frontX, y: bottomY }, camera);
    const LBTop = worldToScreen(canvas, goalDepth, { x: backX, y: topY }, camera);
    const LBBottom = worldToScreen(canvas, goalDepth, { x: backX, y: bottomY }, camera);

    const goalHeightPX = toPixelRadius(canvas, goalDepth, goalHeight);

    const postsColor = "#E0E0E0";
    ctx.strokeStyle = postsColor;
    ctx.lineWidth = toPixelRadius(canvas, goalDepth, postThickness); // scale thickness

    // Draw posts (verticals)
    ctx.beginPath();
    ctx.moveTo(LFTop.x, LFTop.y);
    ctx.lineTo(LFTop.x, LFTop.y - goalHeightPX); // top-left front
    ctx.moveTo(LFBottom.x, LFBottom.y);
    ctx.lineTo(LFBottom.x, LFBottom.y - goalHeightPX); // bottom-left front

    ctx.moveTo(LBTop.x, LBTop.y);
    ctx.lineTo(LBTop.x, LBTop.y - goalHeightPX); // top-left back
    ctx.moveTo(LBBottom.x, LBBottom.y);
    ctx.lineTo(LBBottom.x, LBBottom.y - goalHeightPX); // bottom-left back
    ctx.stroke();

    // Crossbars
    ctx.beginPath();
    ctx.moveTo(LFTop.x, LFTop.y - goalHeightPX);
    ctx.lineTo(LFBottom.x, LFBottom.y - goalHeightPX); // front
    ctx.moveTo(LBTop.x, LBTop.y - goalHeightPX);
    ctx.lineTo(LBBottom.x, LBBottom.y - goalHeightPX); // back
    ctx.moveTo(LBTop.x, LBTop.y);
    ctx.lineTo(LBBottom.x, LBBottom.y); // back lower
    ctx.stroke();

    // Side bars connecting front to back (top and bottom)
    ctx.beginPath();
    ctx.moveTo(LFTop.x, LFTop.y - goalHeightPX);
    ctx.lineTo(LBTop.x, LBTop.y - goalHeightPX); // top
    ctx.moveTo(LFBottom.x, LFBottom.y - goalHeightPX);
    ctx.lineTo(LBBottom.x, LBBottom.y - goalHeightPX); // top-bottom sides
    ctx.moveTo(LFTop.x, LFTop.y);
    ctx.lineTo(LBTop.x, LBTop.y); // bottom front-back
    ctx.moveTo(LFBottom.x, LFBottom.y);
    ctx.lineTo(LBBottom.x, LBBottom.y); // bottom front-back
    ctx.stroke();

    // NET
    const netColor = "#e3e3e390";
    fillPoly(ctx, netColor, [
      {x: LFTop.x, y: LFTop.y - goalHeightPX},
      {x: LFBottom.x, y: LFBottom.y - goalHeightPX},
      {x: LBBottom.x, y: LBBottom.y - goalHeightPX},
      {x: LBTop.x, y: LBTop.y - goalHeightPX},
    ]);
    fillPoly(ctx, netColor, [
      {x: LBTop.x, y: LBTop.y},
      {x: LBBottom.x, y: LBBottom.y},
      {x: LBBottom.x, y: LBBottom.y - goalHeightPX},
      {x: LBTop.x, y: LBTop.y - goalHeightPX},
    ]);
    fillPoly(ctx, netColor, [
      {x: LBTop.x, y: LBTop.y},
      {x: LFTop.x, y: LFTop.y},
      {x: LFTop.x, y: LFTop.y - goalHeightPX},
      {x: LBTop.x, y: LBTop.y - goalHeightPX},
    ]);
    
    fillPoly(ctx, netColor, [
      {x: LBBottom.x, y: LBBottom.y},
      {x: LFBottom.x, y: LFBottom.y},
      {x: LFBottom.x, y: LFBottom.y - goalHeightPX},
      {x: LBBottom.x, y: LBBottom.y - goalHeightPX},
    ]);
  });
}

function scoreToColor(value: number, min: number, max: number): string {
  if (max === min) return "white";
  const t = (value - min) / (max - min);
  const r = Math.floor(255 * t);
  const g = Math.floor(255 * (1 - Math.abs(t - 0.5) * 2));
  const b = Math.floor(255 * (1 - t));
  return `rgb(${r}, ${g}, ${b})`;
}

export default GameRenderer3D;