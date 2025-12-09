import React, { useRef, useEffect, useState } from "react";
import { JsonData, maxX, maxY, Player, Team, Translation2d, Translation3d } from "./types";
import { pitchWidthUnits, pitchHeightUnits } from "./types";

const HEIGHT_SCALE = 0.45;      // Flatten vertical scale for FIFA perspective
const TOP_SCALE = 0.25;         // Top edge scaling (strong perspective)
const BOTTOM_SCALE = 1.0;       // Bottom edge scaling (full width)
const ZOOM = 1.7;               // Medium zoom level
const TILT_Y = 0;               // Camera vertical tilt

interface Camera {
  x: number;
  y: number;
  z: number;
  pitch: number;
  yaw: number;
}

interface GameRendererProps {
  data: JsonData;
}

// Convert a world position in meters to screen position considering perspective and camera
function worldToScreen(
  canvas: HTMLCanvasElement,
  position: Translation3d | Translation2d,
  camera: Camera
): Translation2d {
  // world → camera relative position
  const dx = position.x - camera.x;
  const dy = position.y - camera.y;
  const dz = ("z" in position ? position.z : 0) - camera.z;

  // ---------- 1) Rotate by -yaw around Z ----------
  // yaw = turning left/right (horizontal rotation)
  const cy = Math.cos(-camera.yaw);
  const sy = Math.sin(-camera.yaw);

  const xYaw = dx * cy - dy * sy;
  const yYaw = dx * sy + dy * cy;
  const zYaw = dz;

  // ---------- 2) Rotate by -pitch around X ----------
  // pitch = looking up/down
  const cp = Math.cos(-camera.pitch);
  const sp = Math.sin(-camera.pitch);

  const x1 = xYaw;
  let y1 = yYaw * cp - zYaw * sp;   // forward
  const z1 = yYaw * sp + zYaw * cp;   // up

  // near-plane (avoid division by zero)
  const NEAR = 0.05;
  if (y1 < NEAR) {
    y1 = 0;
  }

  // ---------- 3) Perspective projection ----------
  const fov = radians(90);
  const focal = 1 / Math.tan(fov / 2);

  const aspect = canvas.width / canvas.height;

  const px = (x1 / y1) * focal * ZOOM;
  const py = (z1 / y1) * focal * aspect * ZOOM;

  // ---------- 4) Convert to screen coords ----------
  return {
    x: canvas.width / 2 + px * (canvas.width / 2),
    y: canvas.height / 2 - py * (canvas.height / 2),
  };
}

function radians(angle: number): number {
  return angle * (Math.PI / 180);
}

const GameRenderer3D: React.FC<GameRendererProps> = ({ data }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [camera, setCamera] = useState<Camera>({ x: 0, y: 0, z: 0, pitch: 0, yaw: 0 });

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const goalDepth = (3 / pitchWidthUnits) * canvas.width;

    // Smooth camera follow: limit ball movement and scale
    const cameraX = Math.max(Math.min(data.ball.position.x, 50), -50) * 0.3;

    const cameraY = Math.max(Math.min(data.ball.position.y, 16), -16) * 0.3;

    setCamera({ x: cameraX, y: -80 + cameraY, z: 50, pitch: radians(-33 + (cameraY / 10)), yaw: radians(-cameraX / 10) });
    // const chosenPlayer = data.team1.players[data.team1.teamStrategy.chosenPlayerIndex];
    // const thirdPerson = {x: 3 * chosenPlayer.direction.cos, y: 3 * chosenPlayer.direction.sin}
    // setCamera({x: chosenPlayer.position.x - thirdPerson.x, y: chosenPlayer.position.y - thirdPerson.y, z: 2.5, pitch: radians(-15), yaw: chosenPlayer.direction.value - Math.PI / 2})

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    drawPitch(ctx, canvas, camera);

    // Halfway line
    const halfTop = worldToScreen(canvas, {x: 0, y: maxY}, camera);
    const halfBottom = worldToScreen(canvas, {x: 0, y: -maxY}, camera);
    ctx.beginPath();
    ctx.moveTo(halfTop.x, halfTop.y);
    ctx.lineTo(halfBottom.x, halfBottom.y);
    ctx.stroke();

     // Center circle
    drawCircleWorld(ctx, canvas, { x: 0, y: 0 }, 9.15, "white", camera, false);
    drawCircleWorld(ctx, canvas, { x: 0, y: 0 }, 0.25, "white", camera, true);

    // // Penalty boxes
    drawRectWorld(ctx, canvas, 16.5, 40.3, camera);
    drawRectWorld(ctx, canvas, 16.5, 40.3, camera, true);

    // // Six-yard boxes
    drawRectWorld(ctx, canvas, 5.5, 18.3, camera);
    drawRectWorld(ctx, canvas, 5.5, 18.3, camera, true);

    // // Penalty spots
    drawCircleWorld(ctx, canvas, { x: maxX - 11, y: 0}, 0.25, "white", camera, true);
    drawCircleWorld(ctx, canvas, { x: -(maxX - 11), y: 0}, 0.25, "white", camera, true);

    drawCircleWorld(ctx, canvas, { x: maxX - 16.5, y: 0 }, 9.15, "white", camera, false, 90, 270);
    drawCircleWorld(ctx, canvas, { x: -maxX + 16.5, y: 0 }, 9.15, "white", camera, false, -90, 90);

    // Heatmap
    // drawHeatmap(ctx, canvas, data.team1, camera);

    const draws: { obj: Player | 'ball', depth: number, draw: () => void }[] = [];

    // Ball
    draws.push({
      obj: 'ball',
      depth: getScale(canvas, data.ball.position.y),
      draw: () => drawBall(ctx, canvas, {x: data.ball.position.x, y: data.ball.position.y, z: 0.25}, 0.25, camera),
    });

    // Players
    data.team1.players.forEach((player, i) => {
      draws.push({
        obj: player,
        depth: getScale(canvas, player.position.y),
        draw: () => drawPlayer(ctx, canvas, player, i, data.team1.teamStrategy.chosenPlayerIndex, "red", camera),
      });
    });
    data.team2.players.forEach((player, i) => {
      draws.push({
        obj: player,
        depth: getScale(canvas, player.position.y),
        draw: () => drawPlayer(ctx, canvas, player, i, data.team2.teamStrategy.chosenPlayerIndex, "blue", camera),
      });
    });

    draws.sort((a, b) => b.depth - a.depth);
    draws.forEach(d => d.draw());

    // // Goals
    drawGoals3D(ctx, canvas, camera);

  }, [data]);

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
  position: Translation2d | Translation3d,
  radius: number,
  color: string,
  camera: Camera,
  filled: boolean,
  startAngle = 0,
  endAngle = 360,
) {
  const samples = 32;
  const pts: Translation2d[] = [];
  startAngle = radians(startAngle);
  endAngle = radians(endAngle);

  for (let i = 0; i <= samples; i++) {
    // compute angle proportionally between start and end
    const angle = startAngle + ((i / samples) * (endAngle - startAngle));

    const wx = position.x + Math.cos(angle) * radius;
    const wy = position.y + Math.sin(angle) * radius;

    // project to screen
    const p = worldToScreen(canvas, { x: wx, y: wy, z: "z" in position ? position.z : 0 }, camera);

    pts.push(p);
  }

  if (pts.length < 2) return;

  ctx.beginPath();
  ctx.moveTo(pts[0].x, pts[0].y);

  for (let i = 1; i < pts.length; i++) {
    ctx.lineTo(pts[i].x, pts[i].y);
  }

  if (filled) {
    // optionally connect last point to center for filled arc
    const center = worldToScreen(canvas, position, camera);
    ctx.lineTo(center.x, center.y);
    ctx.closePath();
    ctx.fillStyle = color;
    ctx.fill();
  } else {
    ctx.strokeStyle = color;
    ctx.stroke();
  }
}

function drawBall(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  ball: Translation3d,
  radius: number,
  camera: Camera
) {
  const ballScreen = worldToScreen(canvas, ball, camera);
  const screenRadius = calculateRadius(canvas, ball, radius, camera);

  ctx.beginPath();
  ctx.arc(ballScreen.x, ballScreen.y, screenRadius, 0, Math.PI * 2);
  ctx.fillStyle = "white";
  ctx.fill();
  ctx.strokeStyle = "black";
  ctx.lineWidth = 0.5;
  ctx.stroke(); 
}

function calculateRadius(
  canvas: HTMLCanvasElement,
  position: Translation3d,
  radius: number,
  camera: Camera
): number {
  const center = worldToScreen(canvas, position, camera);

  const dx = position.x - camera.x;
  const dy = position.y - camera.y;
  const dz = position.z - camera.z;

  const fLen = Math.hypot(dx, dy, dz);
  const forward = { x: dx / fLen, y: dy / fLen, z: dz / fLen };

  const upWorld = { x: 0, y: 0, z: 1 };

  let right = {
    x: forward.y * upWorld.z - forward.z * upWorld.y,
    y: forward.z * upWorld.x - forward.x * upWorld.z,
    z: forward.x * upWorld.y - forward.y * upWorld.x
  };

  const rLen = Math.hypot(right.x, right.y, right.z);
  if (rLen < 1e-6) return 0; // camera looking straight up/down
  right.x /= rLen; 
  right.y /= rLen; 
  right.z /= rLen;

  let up = {
    x: right.y * forward.z - right.z * forward.y,
    y: right.z * forward.x - right.x * forward.z,
    z: right.x * forward.y - right.y * forward.x
  };

  const uLen = Math.hypot(up.x, up.y, up.z);
  up.x /= uLen; up.y /= uLen; up.z /= uLen;

  const pRight = worldToScreen(canvas, {
    x: position.x + right.x * radius,
    y: position.y + right.y * radius,
    z: position.z + right.z * radius
  }, camera);

  const pUp = worldToScreen(canvas, {
    x: position.x + up.x * radius,
    y: position.y + up.y * radius,
    z: position.z + up.z * radius
  }, camera);

  const r1 = Math.hypot(pRight.x - center.x, pRight.y - center.y);
  const r2 = Math.hypot(pUp.x - center.x, pUp.y - center.y);
  return Math.max(r1, r2);
}

function drawPlayer(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  player: Player,
  index: number,
  chosenPlayer: number,
  color: string,
  camera: Camera
) {
  // World position of the player
  const pos: Translation3d = { x: player.position.x, y: player.position.y, z: 0 };

  // Screen position at feet
  const screenFeet = worldToScreen(canvas, pos, camera);
  if (!screenFeet) return;

  // ---------- SHADOW ----------
  drawCircleWorld(ctx, canvas, pos, 0.65, "rgba(0,0,0,0.25)", camera, true);

  // ---------- BODY ----------
  const legsHeight = 0.6; // meters
  const bodyHeight = 0.8; // meters
  const bodyRadius = 0.35; // torso radius

  drawCircleWorld(ctx, canvas, {x: pos.x, y: pos.y, z: legsHeight}, bodyRadius, color, camera, true);
  drawCircleWorld(ctx, canvas, {x: pos.x, y: pos.y, z: legsHeight + bodyHeight}, bodyRadius, color, camera, true);
  
  const bodyRadiusPX = calculateRadius(canvas, pos, bodyRadius, camera);
  const bottom = worldToScreen(canvas, {x: pos.x, y: pos.y, z: legsHeight}, camera);
  const bottomLeft = {x: bottom.x - bodyRadiusPX, y: bottom.y};
  const bottomRight = {x: bottom.x + bodyRadiusPX, y: bottom.y};
  
  const top = worldToScreen(canvas, {x: pos.x, y: pos.y, z: legsHeight + bodyHeight}, camera);
  const topLeft = {x: top.x - bodyRadiusPX, y: top.y};
  const topRight = {x: top.x + bodyRadiusPX, y: top.y};

  ctx.beginPath();
  ctx.fillStyle = color;
  ctx.moveTo(bottomLeft.x, bottomLeft.y);
  ctx.lineTo(bottomRight.x, bottomRight.y);
  ctx.lineTo(topRight.x, topRight.y);
  ctx.lineTo(topLeft.x, topLeft.y);
  ctx.fill();

  // ---------- HEAD ----------
  const headHeight = 1.7; // meters from ground
  const headRadius = 0.25; // meters
  const headScreen = worldToScreen(canvas, { x: pos.x, y: pos.y, z: headHeight }, camera);
  const headPixelRadius = calculateRadius(canvas, { x: pos.x, y: pos.y, z: headHeight }, headRadius, camera);

  ctx.beginPath();
  ctx.arc(
    headScreen.x,
    headScreen.y,
    headPixelRadius,
    0,
    Math.PI * 2
  );
  ctx.fillStyle = "#ffe0c4";
  ctx.fill();
  ctx.strokeStyle = "#00000055";
  ctx.stroke();

  // ---------- LEGS (dynamic swing) ----------
  const speed = Math.hypot(player.velocity.x, player.velocity.y);
  const baseStepHz = 1.6;
  const maxStepHz = 3.2;
  const normalizedSpeed = Math.min(speed / 6, 1);
  const stepHz = baseStepHz + (maxStepHz - baseStepHz) * normalizedSpeed;

  // Swing amplitude (in meters)
  const strideLength = 0.35;
  const sideOffset = 0.2;

  const phase = (performance.now() * 0.001 * stepHz) % 1;
  const step = Math.sin(phase * Math.PI * 2);

  // Left leg
  const leftLegX = pos.x - sideOffset;
  const leftLegZ = strideLength * step * normalizedSpeed;
  const leftFootZ = 0;

  const leftLegTop = worldToScreen(canvas, { x: leftLegX, y: pos.y, z: legsHeight + leftLegZ }, camera);
  const leftLegBottom = worldToScreen(canvas, { x: leftLegX, y: pos.y, z: leftFootZ }, camera);

  if (leftLegTop && leftLegBottom) {
    ctx.beginPath();
    ctx.strokeStyle = color;
    ctx.lineWidth = 4;
    ctx.moveTo(leftLegTop.x, leftLegTop.y);
    ctx.lineTo(leftLegBottom.x, leftLegBottom.y);
    ctx.stroke();
  }

  // Right leg (opposite phase)
  const rightLegX = pos.x + sideOffset;
  const rightLegZ = -strideLength * step * normalizedSpeed;
  const rightFootZ = 0;

  const rightLegTop = worldToScreen(canvas, { x: rightLegX, y: pos.y, z: legsHeight + rightLegZ }, camera);
  const rightLegBottom = worldToScreen(canvas, { x: rightLegX, y: pos.y, z: rightFootZ }, camera);

  if (rightLegTop && rightLegBottom) {
    ctx.beginPath();
    ctx.strokeStyle = color;
    ctx.lineWidth = 4;
    ctx.moveTo(rightLegTop.x, rightLegTop.y);
    ctx.lineTo(rightLegBottom.x, rightLegBottom.y);
    ctx.stroke();
  }

  if (index === chosenPlayer) {
    drawCircleWorld(ctx, canvas, pos, bodyRadius * 1.5, "yellow", camera, false, 123, 415);
  }
}

function drawHeatmap(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  team: Team | undefined,
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
      worldToScreen(canvas, { x: entry.key.x - sizeX / 2, y: entry.key.y - sizeY / 2 }, camera),
      worldToScreen(canvas, { x: entry.key.x - sizeX / 2, y: entry.key.y + sizeY / 2 }, camera),
      worldToScreen(canvas, { x: entry.key.x + sizeX / 2, y: entry.key.y + sizeY / 2 }, camera),
      worldToScreen(canvas, { x: entry.key.x + sizeX / 2, y: entry.key.y - sizeY / 2 }, camera)
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
  camera: Camera
) {
  const topLeft = worldToScreen(canvas, {x: -maxX, y: maxY}, camera);
  const topRight = worldToScreen(canvas, {x: maxX, y: maxY}, camera);
  const bottomLeft = worldToScreen(canvas, {x: -maxX, y: -maxY}, camera);
  const bottomRight = worldToScreen(canvas, {x: maxX, y: -maxY}, camera);
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
  widthM: number,
  heightM: number,
  camera: Camera,
  mirror = false
) {
  const xMult = mirror ? -1 : 1;
  const TL = worldToScreen(canvas, {x: xMult * (maxX - widthM), y: heightM / 2, z: 0}, camera);
  const TR = worldToScreen(canvas, {x: xMult * (maxX - widthM), y: -heightM / 2}, camera);
  const BR = worldToScreen(canvas, {x: xMult * (maxX), y: -heightM / 2}, camera);
  const BL = worldToScreen(canvas, {x: xMult * (maxX), y: heightM / 2}, camera);
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
  camera: Camera
) {
  const goalWidth = 7.32; // meters
  const goalHeight = 2.44; // meters
  const goalBackHeight = 2; // meters
  const goalDepth = 3; // meters
  const postThickness = 0.2; // meters, make it wider

  const goals = [
    { frontX: -maxX, backX: -maxX - goalDepth, mirror: false },   // left goal
    { frontX: maxX, backX: maxX + goalDepth, mirror: true }     // right goal
  ];

  goals.forEach(goal => {
    const frontFarDown = worldToScreen(canvas, {x: goal.frontX, y: goalWidth / 2, z: 0}, camera);
    const frontFarUp = worldToScreen(canvas, {x: goal.frontX, y: goalWidth / 2, z: goalHeight}, camera);
    const frontCloseDown = worldToScreen(canvas, {x: goal.frontX, y: -goalWidth / 2, z: 0}, camera);
    const frontCloseUp = worldToScreen(canvas, {x: goal.frontX, y: -goalWidth / 2, z: goalHeight}, camera);

    const backFarDown = worldToScreen(canvas, {x: goal.backX, y: goalWidth / 2, z: 0}, camera);
    const backFarUp = worldToScreen(canvas, {x: goal.backX, y: goalWidth / 2, z: goalBackHeight}, camera);
    const backCloseDown = worldToScreen(canvas, {x: goal.backX, y: -goalWidth / 2, z: 0}, camera);
    const backCloseUp = worldToScreen(canvas, {x: goal.backX, y: -goalWidth / 2, z: goalBackHeight}, camera);

    const postsColor = "#E0E0E0";
    ctx.strokeStyle = postsColor;
    ctx.lineWidth = 3;

    ctx.beginPath();
    ctx.moveTo(frontFarDown.x, frontFarDown.y);
    ctx.lineTo(frontFarUp.x, frontFarUp.y);
    ctx.lineTo(frontCloseUp.x, frontCloseUp.y);
    ctx.lineTo(frontCloseDown.x, frontCloseDown.y);
    ctx.lineTo(backCloseDown.x, backCloseDown.y);
    ctx.lineTo(backFarDown.x, backFarDown.y);
    ctx.lineTo(frontFarDown.x, frontFarDown.y);

    ctx.moveTo(frontFarUp.x, frontFarUp.y);
    ctx.lineTo(backFarUp.x, backFarUp.y);
    ctx.lineTo(backFarDown.x, backFarDown.y);
    
    ctx.moveTo(backFarUp.x, backFarUp.y);
    ctx.lineTo(backCloseUp.x, backCloseUp.y);
    ctx.lineTo(backCloseDown.x, backCloseDown.y);

    ctx.moveTo(backCloseUp.x, backCloseUp.y);
    ctx.lineTo(frontCloseUp.x, frontCloseUp.y);

    ctx.stroke();

    // NET
    const netColor = "#e3e3e390";
    fillPoly(ctx, netColor, [frontFarUp, frontCloseUp, backCloseUp, backFarUp]);
    fillPoly(ctx, netColor, [frontFarUp, frontFarDown, backFarDown, backFarUp]);
    fillPoly(ctx, netColor, [backFarUp, backFarDown, backCloseDown, backCloseUp]);
    fillPoly(ctx, netColor, [frontCloseUp, frontCloseDown, backCloseDown, backCloseUp]);
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
