import React, { useRef, useEffect, useState } from "react";
import { JsonData, maxX, Team, Translation2d } from "./types";
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
    const effectiveBallX = Math.max(Math.min(data.ball.position.x, 25), -25) * 0.5;
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

    // Goals
    drawGoals(ctx, canvas, goalDepth, camera);

    // Ball
    drawCircle(ctx, canvas, data.ball.position, 0.35, goalDepth, "white", camera, true);

    // Players
    drawTeam(ctx, canvas, data.team1, goalDepth, "red", camera);
    drawTeam(ctx, canvas, data.team2, goalDepth, "blue", camera);

    // Heatmap
    drawHeatmap(ctx, canvas, data.team1, goalDepth, camera);

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
  filled: boolean
) {
  const pixelRadius = toPixelRadius(canvas, goalDepth, radiusMeters);
  const screen = worldToScreen(canvas, goalDepth, position, camera);
  // Perspective scaling: farther objects are flatter, closer more circular
  const scale = getScale(canvas, toPixelY(canvas, position.y));
  const radiusX = pixelRadius * ZOOM * scale;
  const radiusY = radiusX * 0.6; // more flattening for distant

  ctx.beginPath();
  ctx.ellipse(screen.x, screen.y, radiusX, radiusY, 0, 0, Math.PI * 2);
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

function drawPlayer(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  position: Translation2d,
  goalDepth: number,
  color: string,
  camera: Camera
) {
  const screen = worldToScreen(canvas, goalDepth, position, camera);

  // Perspective scaling
  const scale = getScale(canvas, toPixelY(canvas, position.y)) * ZOOM;

  const bodyHeight = toPixelRadius(canvas, goalDepth, 1.2) * scale;      // player height
  const bodyWidth = toPixelRadius(canvas, goalDepth, 1) * scale;       // torso width
  const radiusFeetY = bodyWidth / 2 * 0.6; // more flattening for distant
  const headRadius = toPixelRadius(canvas, goalDepth, 0.3) * scale;

  const x = screen.x;
  const y = screen.y;
  const yOffset = 0.7;

  ctx.save();

  // --- BODY ---
  ctx.fillStyle = color;
  ctx.beginPath();
  ctx.roundRect(
    x - bodyWidth / 2,
    y - bodyHeight * yOffset,
    bodyWidth,
    bodyHeight
  );
  ctx.fill();

  // --- FEET ---
  ctx.beginPath();
  ctx.fillStyle = color;
  ctx.ellipse(x, y + radiusFeetY, bodyWidth / 2, radiusFeetY, 0, 0, Math.PI);
  ctx.fill();

  ctx.beginPath();
  ctx.fillStyle = color;
  ctx.ellipse(x, y - bodyHeight * yOffset + 1, bodyWidth / 2, radiusFeetY, 0, Math.PI, Math.PI * 2);
  ctx.fill();

  // --- HEAD ---
  ctx.beginPath();
  ctx.fillStyle = "#ffe0bd";  // light skin tone; you can change
  ctx.arc(x, y - bodyHeight * yOffset - headRadius + 2 * scale, headRadius, 0, Math.PI * 2);
  ctx.fill();

  ctx.restore();
}

function drawTeam(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  team: Team | undefined,
  goalDepth: number,
  color: string,
  camera: Camera
) {
  if (!team) return;
  
  team.players.forEach(player => {
    drawPlayer(ctx, canvas, player.position, goalDepth, color, camera);
  });
}

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
  ctx.fillStyle = "#006400";
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

function drawGoals(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  goalDepth: number,
  camera: Camera
) {
  const goalWidth = (7.3 / pitchHeightUnits) * canvas.height;
  // LEFT GOAL
  ctx.strokeStyle = "red";
  ctx.lineWidth = 4;
  let topLeft = perspectivePoint(
    0,
    (canvas.height - goalWidth) / 2,
    canvas,
    camera
  );
  let topRight = perspectivePoint(
    goalDepth,
    (canvas.height - goalWidth) / 2,
    canvas,
    camera
  );
  let bottomLeft = perspectivePoint(
    0,
    (canvas.height + goalWidth) / 2,
    canvas,
    camera
  );
  let bottomRight = perspectivePoint(
    goalDepth,
    (canvas.height + goalWidth) / 2,
    canvas,
    camera
  );
  ctx.beginPath();
  ctx.moveTo(topLeft.x, topLeft.y);
  ctx.lineTo(bottomLeft.x, bottomLeft.y);
  ctx.lineTo(bottomRight.x, bottomRight.y);
  ctx.lineTo(topRight.x, topRight.y);
  ctx.closePath();
  ctx.stroke();
  // RIGHT GOAL
  ctx.strokeStyle = "blue";
  topLeft = perspectivePoint(
    canvas.width,
    (canvas.height - goalWidth) / 2,
    canvas,
    camera
  );
  topRight = perspectivePoint(
    canvas.width - goalDepth,
    (canvas.height - goalWidth) / 2,
    canvas,
    camera
  );
  bottomLeft = perspectivePoint(
    canvas.width,
    (canvas.height + goalWidth) / 2,
    canvas,
    camera
  );
  bottomRight = perspectivePoint(
    canvas.width - goalDepth,
    (canvas.height + goalWidth) / 2,
    canvas,
    camera
  );
  ctx.beginPath();
  ctx.moveTo(topLeft.x, topLeft.y);
  ctx.lineTo(bottomLeft.x, bottomLeft.y);
  ctx.lineTo(bottomRight.x, bottomRight.y);
  ctx.lineTo(topRight.x, topRight.y);
  ctx.closePath();
  ctx.stroke();
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
