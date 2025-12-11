import React, { useRef, useEffect, useState } from "react";
import { JsonData, maxX, maxY, Player, Team, Translation2d, Translation3d } from "./types";
import { pitchWidthUnits, pitchHeightUnits } from "./types";

const ZOOM = 1.7;               // Medium zoom level

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

let draws: { depth: number, draw: (ctx: CanvasRenderingContext2D, canvas: HTMLCanvasElement) => void }[] = [];

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

    // Smooth camera follow: limit ball movement and scale
    const cameraX = Math.max(Math.min(data.ball.position.x, 50), -50) * 0.3;

    const cameraY = Math.max(Math.min(data.ball.position.y, 16), -16) * 0.1;

    setCamera({ 
      x: cameraX, 
      y: -80 + cameraY, 
      z: 42, 
      pitch: radians(-29 + cameraY), 
      yaw: radians(-cameraX / 2) 
    });
    // const chosenPlayer = data.team1.players[data.team1.teamStrategy.chosenPlayerIndex];
    // const thirdPerson = {x: 6 * chosenPlayer.direction.cos, y: 6 * chosenPlayer.direction.sin}
    // setCamera({x: chosenPlayer.position.x - thirdPerson.x, y: chosenPlayer.position.y - thirdPerson.y, z: 3, pitch: radians(-15), yaw: chosenPlayer.direction.value - Math.PI / 2})

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
    drawCircleWorld(ctx, canvas, { x: 0, y: 0 }, 9.15, "white", camera, 0.15);
    drawCircleWorld(ctx, canvas, { x: 0, y: 0 }, 0.25, "white", camera, 0);

    // Penalty boxes
    drawRectWorld(ctx, canvas, 16.5, 40.3, camera, 0.15);
    drawRectWorld(ctx, canvas, 16.5, 40.3, camera, 0.15, true);

    // Six-yard boxes
    drawRectWorld(ctx, canvas, 5.5, 18.3, camera, 0.15);
    drawRectWorld(ctx, canvas, 5.5, 18.3, camera, 0.15, true);

    // Penalty spots
    drawCircleWorld(ctx, canvas, { x: maxX - 11, y: 0}, 0.25, "white", camera, 0);
    drawCircleWorld(ctx, canvas, { x: -(maxX - 11), y: 0}, 0.25, "white", camera, 0);

    drawCircleWorld(ctx, canvas, { x: maxX - 16.5, y: 0 }, 9.15, "white", camera, 0.15, 90, 270);
    drawCircleWorld(ctx, canvas, { x: -maxX + 16.5, y: 0 }, 9.15, "white", camera, 0.15, -90, 90);

    // Heatmap
    // drawHeatmap(ctx, canvas, data.team1, camera);

    // Ball
    drawBall(ctx, canvas, {x: data.ball.position.x, y: data.ball.position.y, z: 0.25}, 0.25, camera)

    // Players
    data.team1.players.forEach((player, i) => {
      drawPlayer(ctx, canvas, player, i, data.team1.teamStrategy.chosenPlayerIndex, "red", camera);
    });
    data.team2.players.forEach((player, i) => {
      drawPlayer(ctx, canvas, player, i, data.team2.teamStrategy.chosenPlayerIndex, "blue", camera);
    });

    // Goals
    drawGoals3D(ctx, canvas, camera);

    draws.sort((a, b) => b.depth - a.depth);
    draws.forEach(d => d.draw(ctx, canvas));
    draws = [];

  }, [data]);

  return <canvas ref={canvasRef} style={{ backgroundColor: "#006400" }} />;
};

function getDistanceToCamera(position: Translation3d | Translation2d, camera: Camera) {
  const dx = position.x - camera.x;
  const dy = position.y - camera.y;
  const dz = ("z" in position ? position.z : 0) - camera.z;
  return Math.sqrt(dx * dx + dy * dy + dz * dz);
}

function line(camera: Camera, pos1: Translation3d | Translation2d, pos2: Translation3d | Translation2d, color: string | undefined = undefined, lineWidth: number | undefined = undefined) {
  draws.push({
    depth: getDistanceToCamera(pos1, camera),
    draw: (ctx, canvas) => {
      const screen1 = worldToScreen(canvas, pos1, camera);
      const screen2 = worldToScreen(canvas, pos2, camera);

      if (color) ctx.strokeStyle = color;
      if (lineWidth) ctx.lineWidth = calculateRadius(canvas, {...pos1, z: "z" in pos1 ? pos1.z : 0}, lineWidth, camera);

      ctx.beginPath();
      ctx.moveTo(screen1.x, screen1.y);
      ctx.lineTo(screen2.x, screen2.y);
      ctx.stroke();
    }
  });
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
  lineWidth: number,
  startAngle = -3,
  endAngle = 363,
) {
  draws.push({
    depth: getDistanceToCamera(position, camera),
    draw: () => {
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

      ctx.lineWidth = calculateRadius(canvas, {...position, z: "z" in position ? position.z : 0}, lineWidth, camera);
      ctx.beginPath();
      ctx.moveTo(pts[0].x, pts[0].y);

      for (let i = 1; i < pts.length; i++) {
        ctx.lineTo(pts[i].x, pts[i].y);
      }

      if (lineWidth <= 0) {
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
  });
}

function drawBall(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  ball: Translation3d,
  radius: number,
  camera: Camera
) {
  draws.push({
    depth: getDistanceToCamera(ball, camera),
    draw: () => {
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
  });
}

function calculateRadius(
  canvas: HTMLCanvasElement,
  position: Translation3d,
  radius: number,
  camera: Camera
): number {
  const c = worldToScreen(canvas, position, camera);
  if (!c) return 0;

  // Tiny safe offset (small enough to avoid perspective blow-up)
  const EPS = 0.01;

  const pX = worldToScreen(canvas, {
    x: position.x + EPS,
    y: position.y,
    z: position.z
  }, camera);

  const pY = worldToScreen(canvas, {
    x: position.x,
    y: position.y + EPS,
    z: position.z
  }, camera);

  if (!pX || !pY) return 0;

  // Pixel distance for tiny offset
  const dx = Math.hypot(pX.x - c.x, pX.y - c.y);
  const dy = Math.hypot(pY.x - c.x, pY.y - c.y);

  // Pixels per world unit
  const pxPerWU = Math.max(dx, dy) / EPS;

  // Return world radius in pixels
  return pxPerWU * radius;
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
  drawCircleWorld(ctx, canvas, pos, 0.65, "rgba(0,0,0,0.25)", camera, 0);

  // ---------- BODY ----------
  const legsHeight = 0.6; // meters
  const bodyHeight = 0.8; // meters
  const bodyRadius = 0.35; // torso radius

  drawCircleWorld(ctx, canvas, {x: pos.x, y: pos.y, z: legsHeight}, bodyRadius, color, camera, 0);
  drawCircleWorld(ctx, canvas, {x: pos.x, y: pos.y, z: legsHeight + bodyHeight}, bodyRadius, color, camera, 0);
  
  const bodyRadiusPX = calculateRadius(canvas, pos, bodyRadius, camera);
  const bottom = worldToScreen(canvas, {x: pos.x, y: pos.y, z: legsHeight}, camera);
  const bottomLeft = {x: bottom.x - bodyRadiusPX, y: bottom.y};
  const bottomRight = {x: bottom.x + bodyRadiusPX, y: bottom.y};
  
  const top = worldToScreen(canvas, {x: pos.x, y: pos.y, z: legsHeight + bodyHeight}, camera);
  const topLeft = {x: top.x - bodyRadiusPX, y: top.y};
  const topRight = {x: top.x + bodyRadiusPX, y: top.y};

  draws.push({
    depth: getDistanceToCamera({...pos, z: legsHeight}, camera),
    draw: () => {
      ctx.beginPath();
      ctx.fillStyle = color;
      ctx.moveTo(bottomLeft.x, bottomLeft.y);
      ctx.lineTo(bottomRight.x, bottomRight.y);
      ctx.lineTo(topRight.x, topRight.y);
      ctx.lineTo(topLeft.x, topLeft.y);
      ctx.fill();
    }
  })

  // ---------- HEAD ----------
  const headHeight = 1.7; // meters from ground
  const headRadius = 0.25; // meters
  const headScreen = worldToScreen(canvas, {...pos, z: headHeight }, camera);
  const headPixelRadius = calculateRadius(canvas, {...pos, z: headHeight }, headRadius, camera);

  draws.push({
    depth: getDistanceToCamera({...pos, z: headHeight}, camera),
    draw: () => {
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
    }
  });

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

  const leftLegTop = { x: leftLegX, y: pos.y, z: legsHeight + leftLegZ };
  const leftLegBottom = { x: leftLegX, y: pos.y, z: leftFootZ };

  line(camera, leftLegTop, leftLegBottom, color, 0.25);

  // Right leg (opposite phase)
  const rightLegX = pos.x + sideOffset;
  const rightLegZ = -strideLength * step * normalizedSpeed;
  const rightFootZ = 0;

  const rightLegTop = { x: rightLegX, y: pos.y, z: legsHeight + rightLegZ };
  const rightLegBottom = { x: rightLegX, y: pos.y, z: rightFootZ };

  line(camera, rightLegTop, rightLegBottom, color, 0.25);

  if (index === chosenPlayer) {
    drawCircleWorld(ctx, canvas, pos, bodyRadius * 1.5, "yellow", camera, 0.2);
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
    fillPoly(ctx, canvas, color, [
      { x: entry.key.x - sizeX / 2, y: entry.key.y - sizeY / 2 },
      { x: entry.key.x - sizeX / 2, y: entry.key.y + sizeY / 2 },
      { x: entry.key.x + sizeX / 2, y: entry.key.y + sizeY / 2 },
      { x: entry.key.x + sizeX / 2, y: entry.key.y - sizeY / 2 },
    ], camera);
  }
  ctx.restore();
}

function fillPoly(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  color: string,
  points: Translation2d[],
  camera: Camera
) {
  ctx.fillStyle = color;
  ctx.beginPath();
  points = points.map(p => worldToScreen(canvas, p, camera));

  ctx.moveTo(points[0].x, points[0].y);
  for (let i = 1; i < points.length; i++) {
    ctx.lineTo(points[i].x, points[i].y);
  }
  ctx.closePath();
  ctx.fill();
  ctx.stroke();
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
  lineWidth: number,
  mirror = false
) {
  const xMult = mirror ? -1 : 1;
  const pos1 = {x: xMult * (maxX - widthM), y: heightM / 2, z: 0};
  const TL = worldToScreen(canvas, pos1, camera);
  const TR = worldToScreen(canvas, {x: xMult * (maxX - widthM), y: -heightM / 2}, camera);
  const BR = worldToScreen(canvas, {x: xMult * (maxX), y: -heightM / 2}, camera);
  const BL = worldToScreen(canvas, {x: xMult * (maxX), y: heightM / 2}, camera);
  ctx.lineWidth = calculateRadius(canvas, pos1, lineWidth, camera);
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
  const goalBackHeight = 1.9; // meters
  const goalDepth = 2.3; // meters
  const postThickness = 0.15; // meters, make it wider

  const goals = [
    { frontX: -maxX, backX: -maxX - goalDepth, mirror: false },   // left goal
    { frontX: maxX, backX: maxX + goalDepth, mirror: true }     // right goal
  ];

  goals.forEach(goal => {
    const frontFarDown = {x: goal.frontX, y: goalWidth / 2, z: 0};
    const frontFarUp = {x: goal.frontX, y: goalWidth / 2, z: goalHeight};
    const frontCloseDown = {x: goal.frontX, y: -goalWidth / 2, z: 0};
    const frontCloseUp = {x: goal.frontX, y: -goalWidth / 2, z: goalHeight}

    const backFarDown = {x: goal.backX, y: goalWidth / 2, z: 0};
    const backFarUp = {x: goal.backX, y: goalWidth / 2, z: goalBackHeight};
    const backCloseDown = {x: goal.backX, y: -goalWidth / 2, z: 0};
    const backCloseUp = {x: goal.backX, y: -goalWidth / 2, z: goalBackHeight};

    const postsColor = "#E0E0E0";
    ctx.strokeStyle = postsColor;

    // NET
    const netColor = "#e3e3e390";
    draws.push({
      depth: getDistanceToCamera(frontFarUp, camera),
      draw: () => {
        ctx.strokeStyle = postsColor
        ctx.lineWidth = calculateRadius(canvas, frontCloseDown, postThickness, camera);
        fillPoly(ctx, canvas, netColor, [frontFarUp, frontCloseUp, backCloseUp, backFarUp], camera);
        fillPoly(ctx, canvas, netColor, [frontFarUp, frontFarDown, backFarDown, backFarUp], camera);
        fillPoly(ctx, canvas, netColor, [backFarUp, backFarDown, backCloseDown, backCloseUp], camera);
        fillPoly(ctx, canvas, netColor, [frontCloseUp, frontCloseDown, backCloseDown, backCloseUp], camera);
      }
    })
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
