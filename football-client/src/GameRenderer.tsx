import React, { useRef, useEffect, useState } from "react";
import {
  JsonData,
  maxX,
  maxY,
  Player,
  Team,
  Translation2d,
  Translation3d,
  pitchWidth,
  pitchHeight,
  getClientsTeam,
  Camera,
} from "./types";

const ZOOM = 1.5;
const NEAR = 0.05;

interface GameRendererProps {
  data: JsonData;
}

interface Draw {
  pos: Translation3d | Translation2d;
  draw: (ctx: CanvasRenderingContext2D, canvas: HTMLCanvasElement) => void;
}

type CamPoint = { x: number; y: number; z: number };

// =====================
// CAMERA SPACE
// =====================
function worldToCamera(
  position: Translation3d | Translation2d,
  camera: Camera
): CamPoint {
  const dx = position.x - camera.translation.x;
  const dy = position.y - camera.translation.y;
  const dz = ("z" in position ? position.z : 0) - camera.translation.z;

  const cy = Math.cos(-camera.yaw.value);
  const sy = Math.sin(-camera.yaw.value);

  const xYaw = dx * cy - dy * sy;
  const yYaw = dx * sy + dy * cy;
  const zYaw = dz;

  const cp = Math.cos(-camera.pitch.value);
  const sp = Math.sin(-camera.pitch.value);

  return {
    x: xYaw,
    y: yYaw * cp - zYaw * sp,
    z: yYaw * sp + zYaw * cp,
  };
}

function cameraToScreen(canvas: HTMLCanvasElement, p: CamPoint): Translation2d {
  const fov = radians(55);
  const focal = 1 / Math.tan(fov / 2);
  const aspect = canvas.width / canvas.height;

  const x = (((p.x / p.y) * focal) / aspect) * ZOOM;
  const y = (p.z / p.y) * focal * ZOOM;

  return {
    x: canvas.width / 2 + x * (canvas.width / 2),
    y: canvas.height / 2 - y * (canvas.height / 2),
  };
}

// =====================
// SAFE PROJECTION
// =====================
function worldToScreen(
  canvas: HTMLCanvasElement,
  position: Translation3d | Translation2d,
  camera: Camera
): Translation2d | null {
  const cam = worldToCamera(position, camera);
  if (cam.y <= NEAR) return null;
  return cameraToScreen(canvas, cam);
}

// =====================
// CLIPPING
// =====================
function intersect(A: CamPoint, B: CamPoint): CamPoint {
  const t = (NEAR - A.y) / (B.y - A.y);
  return {
    x: A.x + t * (B.x - A.x),
    z: A.z + t * (B.z - A.z),
    y: NEAR,
  };
}

function clipNearPlane(points: CamPoint[]): CamPoint[] {
  const out: CamPoint[] = [];
  for (let i = 0; i < points.length; i++) {
    const A = points[i];
    const B = points[(i + 1) % points.length];

    const Ain = A.y >= NEAR;
    const Bin = B.y >= NEAR;

    if (Ain && Bin) out.push(B);
    else if (Ain && !Bin) out.push(intersect(A, B));
    else if (!Ain && Bin) {
      out.push(intersect(A, B));
      out.push(B);
    }
  }
  return out;
}

// =====================
function radians(a: number) {
  return a * (Math.PI / 180);
}

const GameRenderer3D: React.FC<GameRendererProps> = ({ data }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [camera, setCamera] = useState<Camera>({
    translation: { x: 0, y: 0, z: 0 },
    yaw: { value: 0, cos: 0, sin: 0 },
    pitch: { value: 0, cos: 0, sin: 0 },
  });

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const team = getClientsTeam(data);
    setCamera(team.teamStrategy.cameraManager.position);

    // setCamera({ x: 43, y: 0, z: 5, pitch: radians(-30), yaw: radians(-90)})

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    drawStadium(ctx, canvas, camera);

    drawPitch(ctx, canvas, camera);
    drawLineFlat(
      ctx,
      canvas,
      camera,
      { x: 0, y: maxY },
      { x: 0, y: -maxY },
      "white",
      0.25
    );

    drawCircleWorld(ctx, canvas, camera, { x: 0, y: 0 }, 9.15, "white", 0.25);
    drawCircleWorld(ctx, canvas, camera, { x: 0, y: 0 }, 0.25, "white", 0);

    // Big Rect
    drawRectWorld(ctx, canvas, 16.5, 40.3, camera, 0.25);
    drawRectWorld(ctx, canvas, 16.5, 40.3, camera, 0.25, true);

    // Small Rect
    drawRectWorld(ctx, canvas, 5.5, 18.3, camera, 0.25);
    drawRectWorld(ctx, canvas, 5.5, 18.3, camera, 0.25, true);

    // Penealy spot
    drawCircleWorld(
      ctx,
      canvas,
      camera,
      { x: maxX - 11, y: 0 },
      0.25,
      "white",
      0
    );
    drawCircleWorld(
      ctx,
      canvas,
      camera,
      { x: -(maxX - 11), y: 0 },
      0.25,
      "white",
      0
    );

    // Penelty Arc
    drawCircleWorld(
      ctx,
      canvas,
      camera,
      { x: maxX - 11, y: 0 },
      9.15,
      "white",
      0.25,
      128,
      232
    );
    drawCircleWorld(
      ctx,
      canvas,
      camera,
      { x: -maxX + 11, y: 0 },
      9.15,
      "white",
      0.25,
      -52,
      52
    );

    const draws: Draw[] = [];

    drawGoals3D(ctx, canvas, camera, draws);

    draws.push({
      pos: data.ball.position,
      draw: () => drawBall(ctx, canvas, camera, data.ball.position, 0.2),
    });

    data.team1.players.forEach((p, i) =>
      draws.push({
        pos: p.position,
        draw: () =>
          drawPlayer(
            ctx,
            canvas,
            p,
            i,
            data.team1.teamStrategy.chosenPlayerIndex,
            "red",
            camera
          ),
      })
    );
    data.team2.players.forEach((p, i) =>
      draws.push({
        pos: p.position,
        draw: () =>
          drawPlayer(
            ctx,
            canvas,
            p,
            i,
            data.team2.teamStrategy.chosenPlayerIndex,
            "blue",
            camera
          ),
      })
    );

    draws.sort(
      (a, b) =>
        getDistanceToCamera(b.pos, camera) - getDistanceToCamera(a.pos, camera)
    );
    draws.forEach((d) => d.draw(ctx, canvas));
  }, [data]);

  return <canvas ref={canvasRef} style={{ backgroundColor: "#00a6ffff" }} />;
};

// =====================
// DRAW HELPERS (SAFE)
// =====================
function getDistanceToCamera(p: Translation3d | Translation2d, c: Camera) {
  const dx = p.x - c.translation.x;
  const dy = p.y - c.translation.y;
  const dz = ("z" in p ? p.z : 0) - c.translation.z;
  return Math.sqrt(dx * dx + dy * dy + dz * dz);
}

function drawLineFlat(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  camera: Camera,
  p1: Translation3d | Translation2d,
  p2: Translation3d | Translation2d,
  color: string,
  widthWorld: number
) {
  const p1z = "z" in p1 ? p1.z : 0;
  const p2z = "z" in p2 ? p2.z : 0;

  const dir = {
    x: p2.x - p1.x,
    y: p2.y - p1.y,
    z: p2z - p1z,
  };

  const perp = getPerpVector(dir, { x: 0, y: 0, z: 1 });

  const width = widthWorld / 2;
  const offset = { x: perp.x * width, y: perp.y * width, z: perp.z * width };

  const pts: Translation3d[] = [
    { x: p1.x + offset.x, y: p1.y + offset.y, z: p1z + offset.z },
    { x: p1.x - offset.x, y: p1.y - offset.y, z: p1z - offset.z },
    { x: p2.x - offset.x, y: p2.y - offset.y, z: p2z - offset.z },
    { x: p2.x + offset.x, y: p2.y + offset.y, z: p2z + offset.z },
  ];

  fillPoly(ctx, canvas, pts, camera, color);
}

function drawLine3d(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  camera: Camera,
  p1: Translation3d,
  p2: Translation3d,
  color: string,
  widthWorld: number
) {
  const p1z = "z" in p1 ? p1.z : 0;
  const p2z = "z" in p2 ? p2.z : 0;

  const dir = {
    x: p2.x - p1.x,
    y: p2.y - p1.y,
    z: p2z - p1z,
  };

  const dirFromCamera = {
    x: p2.x - camera.translation.x,
    y: p2.y - camera.translation.y,
    z: p2z - camera.translation.z,
  };

  const perp = getPerpVector(dir, dirFromCamera);

  const width = widthWorld / 2;
  const offset = { x: perp.x * width, y: perp.y * width, z: perp.z * width };

  const pts: Translation3d[] = [
    { x: p1.x + offset.x, y: p1.y + offset.y, z: p1z + offset.z },
    { x: p1.x - offset.x, y: p1.y - offset.y, z: p1z - offset.z },
    { x: p2.x - offset.x, y: p2.y - offset.y, z: p2z - offset.z },
    { x: p2.x + offset.x, y: p2.y + offset.y, z: p2z + offset.z },
  ];

  fillPoly(ctx, canvas, pts, camera, color);
  drawSphere(ctx, canvas, camera, p1, width, color, false);
  drawSphere(ctx, canvas, camera, p2, width, color, false);
}

function getPerpVector(dir: Translation3d, u: Translation3d): Translation3d {
  let up: Translation3d = u;

  // Cross product: perp = dir × up
  const perp: Translation3d = {
    x: dir.y * up.z - dir.z * up.y,
    y: dir.z * up.x - dir.x * up.z,
    z: dir.x * up.y - dir.y * up.x,
  };

  // Normalize
  const len = Math.hypot(perp.x, perp.y, perp.z);
  if (len === 0) return { x: 1, y: 0, z: 0 }; // fallback
  return { x: perp.x / len, y: perp.y / len, z: perp.z / len };
}

function strokePoly3d(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  points: Translation3d[],
  camera: Camera,
  color: string,
  width: number,
  closePath: boolean = true
) {
  for (let i = 0; i < points.length; i++) {
    if (!closePath && i == points.length - 1) continue;
    drawLine3d(
      ctx,
      canvas,
      camera,
      points[i],
      points[(i + 1) % points.length],
      color,
      width
    );
  }
}

function strokePoly(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  points: (Translation2d | Translation3d)[],
  camera: Camera,
  color: string,
  width: number,
  closePath: boolean = true
) {
  for (let i = 0; i < points.length; i++) {
    if (!closePath && i == points.length - 1) continue;
    drawLineFlat(
      ctx,
      canvas,
      camera,
      points[i],
      points[(i + 1) % points.length],
      color,
      width
    );
  }
}

function fillPoly(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  points: (Translation2d | Translation3d)[],
  camera: Camera,
  fillColor: string
) {
  const camPts = points.map((p) => worldToCamera(p, camera));
  const clipped = clipNearPlane(camPts);
  if (clipped.length < 3) return;

  const screen = clipped.map((p) => cameraToScreen(canvas, p));
  ctx.beginPath();
  ctx.moveTo(screen[0].x, screen[0].y);
  for (let i = 1; i < screen.length; i++) ctx.lineTo(screen[i].x, screen[i].y);
  ctx.closePath();
  ctx.fillStyle = fillColor;
  ctx.fill();
}

function drawPitch(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  camera: Camera
) {
  const corners: Translation3d[] = [
    { x: -maxX, y: maxY, z: 0 },
    { x: maxX, y: maxY, z: 0 },
    { x: maxX, y: -maxY, z: 0 },
    { x: -maxX, y: -maxY, z: 0 },
  ];

  fillPoly(ctx, canvas, corners, camera, "#0a7f2e");

  const stripeCount = 18;
  const stripeWidth = pitchWidth / stripeCount;

  for (let i = 0; i < stripeCount; i++) {
    const xLeft = -maxX + i * stripeWidth;
    const xRight = xLeft + stripeWidth;

    const stripeCorners: Translation3d[] = [
      { x: xLeft, y: maxY, z: 0 },
      { x: xRight, y: maxY, z: 0 },
      { x: xRight, y: -maxY, z: 0 },
      { x: xLeft, y: -maxY, z: 0 },
    ];

    const color = i % 2 === 0 ? "#0b8a33" : "#0a7f2e";
    fillPoly(ctx, canvas, stripeCorners, camera, color);
  }

  const lineWidth = 0.2;
  const halfWidth = lineWidth / 2;

  strokePoly(
    ctx,
    canvas,
    corners.map((p) => ({
      x: p.x - halfWidth * Math.sign(p.x),
      y: p.y - halfWidth * Math.sign(p.y),
      z: 0,
    })),
    camera,
    "white",
    lineWidth
  );
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
  const pos1 = { x: xMult * (maxX - widthM), y: heightM / 2, z: 0 };
  const corners = [
    pos1,
    { x: xMult * (maxX - widthM), y: -heightM / 2 },
    { x: xMult * maxX, y: -heightM / 2 },
    { x: xMult * maxX, y: heightM / 2 },
  ];
  strokePoly(ctx, canvas, corners, camera, "white", lineWidth);
}

// -----------------------
// Drawing helpers
// -----------------------
function drawCircleWorld(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  camera: Camera,
  position: Translation2d | Translation3d,
  radius: number,
  color: string,
  lineWidth: number,
  startAngle = 0,
  endAngle = 360
) {
  const samples = 32;
  const pts: Translation3d[] = [];
  startAngle = radians(startAngle);
  endAngle = radians(endAngle);

  for (let i = 0; i <= samples; i++) {
    // compute angle proportionally between start and end
    const angle = startAngle + (i / samples) * (endAngle - startAngle);

    const wx = position.x + Math.cos(angle) * radius;
    const wy = position.y + Math.sin(angle) * radius;

    // project to screen
    pts.push({ x: wx, y: wy, z: "z" in position ? position.z : 0 });
  }

  if (lineWidth <= 0) {
    fillPoly(ctx, canvas, pts, camera, color);
  } else {
    strokePoly(ctx, canvas, pts, camera, color, lineWidth, false);
  }
}

function drawSphere(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  camera: Camera,
  position: Translation3d,
  radius: number,
  fillColor: string,
  stroke: boolean = true
) {
  // Project the center
  const center = worldToScreen(canvas, position, camera);
  if (!center) return;

  // Camera right vector (local X axis in world space)
  const rightOffset: Translation3d = {
    x: position.x + radius * camera.yaw.cos,
    y: position.y + radius * camera.yaw.sin,
    z: position.z,
  };

  // Camera up vector (local Z axis in world space)
  const upOffset: Translation3d = {
    x: position.x,
    y: position.y,
    z: position.z + radius,
  };

  const screenRight = worldToScreen(canvas, rightOffset, camera);
  const screenUp = worldToScreen(canvas, upOffset, camera);

  if (!screenRight || !screenUp) return;

  // Compute pixel radius as max distance from center
  const pixelRadius = Math.max(
    Math.hypot(screenRight.x - center.x, screenRight.y - center.y),
    Math.hypot(screenUp.x - center.x, screenUp.y - center.y)
  );

  ctx.beginPath();
  ctx.arc(center.x, center.y, pixelRadius, 0, Math.PI * 2);
  ctx.fillStyle = fillColor;
  ctx.fill();
  if (stroke) {
    ctx.strokeStyle = "#00000055";
    ctx.lineWidth = 0.5;
    ctx.stroke();
  }
}

function drawBall(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  camera: Camera,
  position: Translation3d,
  radius: number
) {
  drawCircleWorld(
    ctx,
    canvas,
    camera,
    { ...position, z: 0 },
    radius + 0.05,
    "rgba(0,0,0,0.25)",
    0
  );
  drawSphere(ctx, canvas, camera, position, radius, "white");
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
  const pos: Translation3d = {
    x: player.position.x,
    y: player.position.y,
    z: 0,
  };

  const legsHeight = 0.6; // meters
  const bodyHeight = 0.8;
  const bodyRadius = 0.28;

  // ---------- SHADOW ----------
  drawCircleWorld(ctx, canvas, camera, pos, 0.55, "rgba(0,0,0,0.25)", 0);

  // ---------- HIGHLIGHT CHOSEN PLAYER ----------
  if (index === chosenPlayer) {
    drawCircleWorld(ctx, canvas, camera, pos, bodyRadius * 1.5, "yellow", 0.2);
  }

  // ---------- BODY ----------
  const torsoBottom = { x: pos.x, y: pos.y, z: legsHeight };
  const torsoTop = { x: pos.x, y: pos.y, z: legsHeight + bodyHeight };

  drawCylinder(
    ctx,
    canvas,
    camera,
    torsoBottom,
    torsoTop,
    bodyRadius * 1.02,
    color
  );

  // ---------- HEAD ----------
  const headHeight = legsHeight + bodyHeight + 0.25;
  const headRadius = 0.25;
  drawSphere(
    ctx,
    canvas,
    camera,
    { x: pos.x, y: pos.y, z: headHeight },
    headRadius,
    "#ffe0c4"
  );

  // ---------- LEGS ----------
  const speed = Math.hypot(player.velocity.x, player.velocity.y);
  const normalizedSpeed = Math.min(speed / 6, 1);
  const baseStepHz = 1.6;
  const maxStepHz = 3.2;
  const stepHz = baseStepHz + (maxStepHz - baseStepHz) * normalizedSpeed;
  const phase = (performance.now() * 0.001 * stepHz) % 1;
  const step = Math.sin(phase * 2 * Math.PI);
  const strideLength = 0.35;
  const sideOffset = 0.15;

  const legRadius = 0.12;

  // Left leg
  const leftX = pos.x - sideOffset * player.direction.sin;
  const leftY = pos.y + sideOffset * player.direction.cos;
  const leftTop: Translation3d = { x: leftX, y: leftY, z: legsHeight };
  const leftBottom: Translation3d = {
    x: leftX + strideLength * step * normalizedSpeed * player.direction.cos,
    y: leftY + strideLength * step * normalizedSpeed * player.direction.sin,
    z: 0,
  };
  drawCylinder(ctx, canvas, camera, leftTop, leftBottom, legRadius, color);

  // Right leg (opposite phase)
  const rightX = pos.x + sideOffset * player.direction.sin;
  const rightY = pos.y - sideOffset * player.direction.cos;
  const rightTop: Translation3d = { x: rightX, y: rightY, z: legsHeight };
  const rightBottom: Translation3d = {
    x: rightX - strideLength * step * normalizedSpeed * player.direction.cos,
    y: rightY - strideLength * step * normalizedSpeed * player.direction.sin,
    z: 0,
  };
  drawCylinder(ctx, canvas, camera, rightTop, rightBottom, legRadius, color);
}

function drawCylinder(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  camera: Camera,
  bottom: Translation3d,
  top: Translation3d,
  radius: number,
  fillColor: string,
  stroke: boolean = false
) {
  // Project center points
  const screenBottom = worldToScreen(canvas, bottom, camera);
  const screenTop = worldToScreen(canvas, top, camera);
  if (!screenBottom || !screenTop) return;

  // Camera right vector at bottom
  const rightBottom: Translation3d = {
    x: bottom.x + radius * camera.yaw.cos,
    y: bottom.y + radius * camera.yaw.sin,
    z: bottom.z,
  };

  const rightTop: Translation3d = {
    x: top.x + radius * camera.yaw.cos,
    y: top.y + radius * camera.yaw.sin,
    z: top.z,
  };

  const screenRightBottom = worldToScreen(canvas, rightBottom, camera);
  const screenRightTop = worldToScreen(canvas, rightTop, camera);
  if (!screenRightBottom || !screenRightTop) return;

  // Camera up vector at bottom (just for pixel width)
  const upBottom: Translation3d = {
    x: bottom.x,
    y: bottom.y,
    z: bottom.z + radius,
  };
  const screenUpBottom = worldToScreen(canvas, upBottom, camera);
  if (!screenUpBottom) return;

  // Pixel width of cylinder
  const pixelWidth = Math.max(
    Math.hypot(
      screenRightBottom.x - screenBottom.x,
      screenRightBottom.y - screenBottom.y
    ),
    Math.hypot(
      screenUpBottom.x - screenBottom.x,
      screenUpBottom.y - screenBottom.y
    )
  );

  // Draw rectangle between bottom and top
  const dx = screenTop.y - screenBottom.y;
  const dy = screenBottom.x - screenTop.x;
  const len = Math.hypot(dx, dy);
  const nx = (dx / len) * pixelWidth;
  const ny = (dy / len) * pixelWidth;

  const poly: Translation2d[] = [
    { x: screenBottom.x - nx, y: screenBottom.y - ny },
    { x: screenBottom.x + nx, y: screenBottom.y + ny },
    { x: screenTop.x + nx, y: screenTop.y + ny },
    { x: screenTop.x - nx, y: screenTop.y - ny },
  ];

  // Fill polygon
  ctx.beginPath();
  ctx.moveTo(poly[0].x, poly[0].y);
  for (let i = 1; i < poly.length; i++) ctx.lineTo(poly[i].x, poly[i].y);
  ctx.closePath();
  ctx.fillStyle = fillColor;
  ctx.fill();

  drawCircleWorld(ctx, canvas, camera, bottom, radius, fillColor, 0);
  drawCircleWorld(ctx, canvas, camera, top, radius, fillColor, 0);

  if (stroke) {
    ctx.strokeStyle = "#00000055";
    ctx.lineWidth = 0.5;
    ctx.stroke();
  }
}

function drawHeatmap(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  team: Team | undefined,
  camera: Camera
) {
  if (!team) return;
  const scoreValues = team.teamStrategy.scores.map((score) => score.value);
  const maxValue = Math.min(Math.max(...scoreValues), 50);
  ctx.save();
  ctx.globalAlpha = 0.3;
  const sizeX = pitchWidth / 40;
  const sizeY = pitchHeight / 40;
  for (const entry of team.teamStrategy.scores) {
    const color = scoreToColor(entry.value, -120, maxValue);
    fillPoly(
      ctx,
      canvas,
      [
        { x: entry.key.x - sizeX / 2, y: entry.key.y - sizeY / 2 },
        { x: entry.key.x - sizeX / 2, y: entry.key.y + sizeY / 2 },
        { x: entry.key.x + sizeX / 2, y: entry.key.y + sizeY / 2 },
        { x: entry.key.x + sizeX / 2, y: entry.key.y - sizeY / 2 },
      ],
      camera,
      color
    );
  }
  ctx.restore();
}

function drawGoals3D(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  camera: Camera,
  draws: Draw[]
) {
  const goalWidth = 7.32; // meters
  const goalHeight = 2.44; // meters
  const goalBackHeight = 1.9; // meters
  const goalDepth = 2.3; // meters
  const postThickness = 0.18; // meters

  const goals = [
    { frontX: -maxX, backX: -maxX - goalDepth, mirror: false }, // left goal
    { frontX: maxX, backX: maxX + goalDepth, mirror: true }, // right goal
  ];

  goals.forEach((goal) => {
    const frontFarDown = { x: goal.frontX, y: goalWidth / 2, z: 0 };
    const frontFarUp = { x: goal.frontX, y: goalWidth / 2, z: goalHeight };
    const frontCloseDown = { x: goal.frontX, y: -goalWidth / 2, z: 0 };
    const frontCloseUp = { x: goal.frontX, y: -goalWidth / 2, z: goalHeight };

    const backFarDown = { x: goal.backX, y: goalWidth / 2, z: 0 };
    const backFarUp = { x: goal.backX, y: goalWidth / 2, z: goalBackHeight };
    const backCloseDown = { x: goal.backX, y: -goalWidth / 2, z: 0 };
    const backCloseUp = { x: goal.backX, y: -goalWidth / 2, z: goalBackHeight };

    const postsColor = "#E0E0E0";
    const netColor = "#e3e3e390";
    draws.push({
      pos: backCloseUp,
      draw: () => {
        fillPoly(
          ctx,
          canvas,
          [frontFarUp, frontCloseUp, backCloseUp, backFarUp],
          camera,
          netColor
        );
        strokePoly3d(
          ctx,
          canvas,
          [frontFarUp, frontCloseUp, backCloseUp, backFarUp],
          camera,
          postsColor,
          postThickness
        );
      },
    });
    draws.push({
      pos: backFarUp,
      draw: () => {
        fillPoly(
          ctx,
          canvas,
          [frontFarUp, frontFarDown, backFarDown, backFarUp],
          camera,
          netColor
        );
        strokePoly3d(
          ctx,
          canvas,
          [frontFarUp, frontFarDown, backFarDown, backFarUp],
          camera,
          postsColor,
          postThickness
        );
      },
    });
    draws.push({
      pos: backFarUp,
      draw: () => {
        fillPoly(
          ctx,
          canvas,
          [backFarUp, backFarDown, backCloseDown, backCloseUp],
          camera,
          netColor
        );
        strokePoly3d(
          ctx,
          canvas,
          [backFarUp, backFarDown, backCloseDown, backCloseUp],
          camera,
          postsColor,
          postThickness
        );
      },
    });
    draws.push({
      pos: backCloseUp,
      draw: () => {
        fillPoly(
          ctx,
          canvas,
          [frontCloseUp, frontCloseDown, backCloseDown, backCloseUp],
          camera,
          netColor
        );
        strokePoly3d(
          ctx,
          canvas,
          [frontCloseUp, frontCloseDown, backCloseDown, backCloseUp],
          camera,
          postsColor,
          postThickness
        );
      },
    });
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


function drawStadium(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  camera: Camera
) {
  const wallHeight = 7;
  const standDepth = 12;

  const tiers = 16;
  const tierHeight = 0.7;
  const tierDepth = 1.4;

  const seatWidth = 0.8;
  const seatGap = 0.3;
  const seatHeight = 0.6;

  const wallColor = "#555555";
  const standColor = "#888888";

  const seatColors = ["#d32f2f", "#1976d2", "#388e3c", "#7b1fa2"];
  const crowdColors = ["#ffcc99", "#f4a460", "#ffd700", "#ffb6c1"];

  // =====================
  // MATCH LIGHTING CONFIG
  // =====================
  const isNightMatch = false;
  const sunDir = { x: -0.6, y: 0.8 }; // directional sunlight
  const sunStrength = isNightMatch ? 0 : 50; // visible gradient

  function shadeColor(hex: string, amount: number) {
    const num = parseInt(hex.slice(1), 16);
    const r = Math.max(40, Math.min(255, (num >> 16) + amount));
    const g = Math.max(40, Math.min(255, ((num >> 8) & 0xff) + amount));
    const b = Math.max(40, Math.min(255, (num & 0xff) + amount));
    return `rgb(${r},${g},${b})`;
  }

  // =====================
  // WALLS
  // =====================
  const stadiumCorners = [
    { x: maxX + standDepth, y: maxY + standDepth, z: 0 },
    { x: maxX + standDepth, y: -(maxY + standDepth), z: 0 },
    { x: -(maxX + standDepth), y: -(maxY + standDepth), z: 0 },
    { x: -(maxX + standDepth), y: maxY + standDepth, z: 0 },
  ];

  fillPoly(
    ctx,
    canvas,
    stadiumCorners,
    camera,
    isNightMatch ? "#1b3a2a" : "#2e7d32"
  );

  // =====================
  // WALLS with 3D vertical shading
  // =====================
  for (let i = 0; i < stadiumCorners.length; i++) {
    const c1 = stadiumCorners[i];
    const c2 = stadiumCorners[(i + 1) % stadiumCorners.length];

    // Compute wall normal in XY plane
    const dx = c2.x - c1.x;
    const dy = c2.y - c1.y;
    const length = Math.hypot(dx, dy);
    const nx = -dy / length; // perpendicular x
    const ny = dx / length;  // perpendicular y

    // Dot with sun direction for horizontal shading
    const dot = Math.max(0, nx * sunDir.x + ny * sunDir.y);
    const baseShade = Math.floor(dot * sunStrength) - 20; // horizontal light/dark

    const steps = 10; // number of vertical steps for shading
    const stepHeight = wallHeight / steps;

    for (let s = 0; s < steps; s++) {
      const zBottom = s * stepHeight;
      const zTop = (s + 1) * stepHeight;

      // Linear vertical gradient: darker at bottom, lighter at top
      const verticalOffset = Math.floor((s / steps) * 30); // tweak 30 for stronger vertical contrast
      const color = shadeColor(wallColor, baseShade + verticalOffset);

      fillPoly(
        ctx,
        canvas,
        [
          { ...c1, z: zBottom },
          { ...c2, z: zBottom },
          { ...c2, z: zTop },
          { ...c1, z: zTop },
        ],
        camera,
        color
      );
    }

    // Optional: thin top highlight
    fillPoly(
      ctx,
      canvas,
      [
        { ...c1, z: wallHeight },
        { ...c2, z: wallHeight },
        { ...c2, z: wallHeight + 0.2 },
        { ...c1, z: wallHeight + 0.2 },
      ],
      camera,
      shadeColor("#ffffff", 20)
    );
  }

  // =====================
  // STANDS
  // =====================
  const sides = [
    { start: { x: -(maxX + standDepth), y: maxY }, end: { x: maxX + standDepth, y: maxY }, dir: { x: 0, y: 1 }, home: true },
    { start: { x: -(maxX + standDepth), y: -maxY }, end: { x: maxX + standDepth, y: -maxY }, dir: { x: 0, y: -1 }, home: false },
    { start: { x: -maxX, y: -(maxY + standDepth) }, end: { x: -maxX, y: maxY + standDepth }, dir: { x: -1, y: 0 }, home: true },
    { start: { x: maxX, y: -(maxY + standDepth) }, end: { x: maxX, y: maxY + standDepth }, dir: { x: 1, y: 0 }, home: false },
  ];

  sides.forEach((side) => {
    const dx = side.end.x - side.start.x;
    const dy = side.end.y - side.start.y;
    const length = Math.hypot(dx, dy);
    const seatsPerRow = Math.floor(length / (seatWidth + seatGap));
    const tangent = { x: dx / length, y: dy / length };

    // Draw stand floor
    fillPoly(
      ctx,
      canvas,
      [
        { x: side.start.x + side.dir.x * standDepth, y: side.start.y + side.dir.y * standDepth, z: wallHeight },
        { x: side.end.x + side.dir.x * standDepth, y: side.end.y + side.dir.y * standDepth, z: wallHeight },
        { x: side.end.x + side.dir.x * (standDepth + tiers * tierDepth), y: side.end.y + side.dir.y * (standDepth + tiers * tierDepth), z: wallHeight + tiers * tierHeight },
        { x: side.start.x + side.dir.x * (standDepth + tiers * tierDepth), y: side.start.y + side.dir.y * (standDepth + tiers * tierDepth), z: wallHeight + tiers * tierHeight },
      ],
      camera,
      shadeColor(standColor, (isNightMatch ? -40 : -25))
    );

    for (let tier = 0; tier < tiers; tier++) {
      const z = wallHeight + tier * tierHeight;
      const depthOffset = standDepth + tier * tierDepth;

      // Vertical tier darkening
      const tierOffset = -tier * 3;

      for (let i = 1; i < seatsPerRow; i++) {
        const t = i / seatsPerRow;
        const baseX = side.start.x + dx * t + side.dir.x * depthOffset;
        const baseY = side.start.y + dy * t + side.dir.y * depthOffset;

        const baseSeatColor = seatColors[(i + tier * 2) % seatColors.length];
        const sectionBoost = side.home ? 8 : -6;

        // =====================
        // Calculate combined gradient for this seat
        // =====================
        const seatLength = Math.hypot(baseX, baseY);
        const seatDir = { x: baseX / seatLength, y: baseY / seatLength };
        const sunDot = seatDir.x * sunDir.x + seatDir.y * sunDir.y;
        const sunOffset = Math.floor(sunDot * sunStrength);

        // Radial gradient from pitch center (closer seats are brighter)
        const distanceFromPitch = Math.hypot(baseX, baseY);
        const maxDistance = maxX + standDepth + tiers * tierDepth;
        const radialFactor = Math.max(0, 1 - distanceFromPitch / maxDistance); // 0..1
        const radialOffset = Math.floor(radialFactor * 20); // adjust intensity

        // Total combined offset
        const totalOffset = tierOffset + sectionBoost + sunOffset + radialOffset;

        const seatBase = shadeColor(baseSeatColor, totalOffset);
        const seatFront = shadeColor(baseSeatColor, tierOffset - 12 + sunOffset + radialOffset);

        const halfW = seatWidth * 0.5;
        const manW = seatWidth * 0.25;
        const depth = seatWidth * 0.8;

        const seatPoly: Translation3d[] = [
          { x: baseX - tangent.x * halfW, y: baseY - tangent.y * halfW, z },
          { x: baseX + tangent.x * halfW, y: baseY + tangent.y * halfW, z },
          { x: baseX + tangent.x * halfW + side.dir.x * depth, y: baseY + tangent.y * halfW + side.dir.y * depth, z },
          { x: baseX - tangent.x * halfW + side.dir.x * depth, y: baseY - tangent.y * halfW + side.dir.y * depth, z },
        ];

        fillPoly(ctx, canvas, seatPoly, camera, seatBase);
        fillPoly(
          ctx,
          canvas,
          [
            seatPoly[2],
            seatPoly[3],
            { ...seatPoly[3], z: z + seatHeight },
            { ...seatPoly[2], z: z + seatHeight },
          ],
          camera,
          seatFront
        );

        const crowdZ = z + 0.3 + Math.abs(Math.sin(performance.now() / 200 + tier)) * 0.3;

        const crowdTierFactor = tier / tiers;
        const crowdTierOffset = Math.floor(crowdTierFactor * 15);
        const crowdTotalOffset = totalOffset + crowdTierOffset;

        fillPoly(
          ctx,
          canvas,
          [
            { x: baseX + tangent.x * manW + side.dir.x * depth, y: baseY + tangent.y * manW + side.dir.y * depth, z },
            { x: baseX - tangent.x * manW + side.dir.x * depth, y: baseY - tangent.y * manW + side.dir.y * depth, z },
            { x: baseX - tangent.x * manW + side.dir.x * depth, y: baseY - tangent.y * manW + side.dir.y * depth, z: crowdZ },
            { x: baseX + tangent.x * manW + side.dir.x * depth, y: baseY + tangent.y * manW + side.dir.y * depth, z: crowdZ },
          ],
          camera,
          shadeColor(crowdColors[(i + tier) % crowdColors.length], (isNightMatch ? 10 : 0) + crowdTotalOffset)
        );
      }
    }
  });
}


export default GameRenderer3D;
