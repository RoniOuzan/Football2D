import type { Camera } from "../client/jsons/gameTypes";
import type { Translation3d, Translation2d } from "../types";
import { maxX, maxY, pitchWidth } from "../types";
import { worldToCamera, clipNearPlane, cameraToScreen, worldToScreen } from "./CameraUtil";
import { radians } from "./GameRenderer";

export function drawLineFlat(
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

export function drawLine3d(
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

export function getPerpVector(dir: Translation3d, u: Translation3d): Translation3d {
  const up: Translation3d = u;

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

export function strokePoly3d(
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

export function strokePoly(
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

export function fillPoly(
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

export function drawPitch(
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

export function drawRectWorld(
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
export function drawCircleWorld(
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

export function drawSphere(
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

export function drawCylinder(
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