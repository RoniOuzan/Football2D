import type { Camera } from "../types";
import { maxX, maxY } from "../types";
import { isOnScreen, worldToScreen } from "./CameraUtil";
import { fillPoly } from "./RendererUtil";

const sunWorld = { x: 200, y: 200, z: 80 };

const wallHeight = 1.5;
const standDepth = 20;

const tiers = 16;
const tierHeight = 1;
const tierDepth = 1.6;

const seatWidth = 1.2;
const seatGap = 0.2;
const seatHeight = 0.7;

const wallColor = "#555555";
const standColor = "#888888";

const seatColors = ["#d32f2f", "#1976d2", "#388e3c", "#7b1fa2"];
const crowdColors = ["#ffcc99", "#f4a460", "#ffd700", "#ffb6c1"];

const isNightMatch = false;
const sunDir = {
  x: -sunWorld.x,
  y: -sunWorld.y,
};
const sunLen = Math.hypot(sunDir.x, sunDir.y);
sunDir.x /= sunLen;
sunDir.y /= sunLen;
const sunStrength = isNightMatch ? 0 : 30; 

export function drawStadium(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  camera: Camera
) {
  drawSun(ctx, canvas, camera);

  // =====================
  // ELLIPSE HELPERS
  // =====================
  const baseA = maxX + standDepth;
  const baseB = maxY + standDepth;

  // =====================
  // PITCH / BASE
  // =====================
  fillPoly(
    ctx,
    canvas,
    ellipsePoints(baseA, baseB, 0),
    camera,
    isNightMatch ? "#1b3a2a" : "#2e7d32"
  );

  // =====================
  // STAND FLOOR (GRAY RING)
  // =====================
  drawCone(ctx, canvas, camera, baseA, baseB, tiers * tierDepth, wallHeight, tiers * tierHeight, standColor);

  // =====================
  // WALLS (ELLIPSE EXTRUSION)
  // =====================
  drawWalls(ctx, canvas, camera, baseA, baseB, 0, wallHeight, wallColor);

  // =====================
  // STANDS + SEATS (ELLIPSE)
  // =====================
  for (let tier = 0; tier < tiers; tier++) {
    const z = wallHeight + 0.2 + tier * tierHeight;
    const a = baseA + tier * tierDepth;
    const b = baseB + tier * tierDepth;
    const perimeter = ellipsePerimeter(a, b);

    const angularSeats = perimeter / (seatWidth + seatGap);
    for (let i = 0; i < angularSeats; i++) {
      const t = (i / angularSeats) * Math.PI * 2;

      const baseX = a * Math.cos(t);
      const baseY = b * Math.sin(t);

      // Tangent
      const tx = -a * Math.sin(t);
      const ty = b * Math.cos(t);
      const tLen = Math.hypot(tx, ty);
      const tangent = { x: tx / tLen, y: ty / tLen };

      // Normal
      const nLen = Math.hypot(baseX, baseY);
      const normal = { x: baseX / nLen, y: baseY / nLen };

      // Approximate center of seat for visibility check
      const seatCenter = {
        x: baseX + normal.x * (seatWidth * 0.8 * 0.5),
        y: baseY + normal.y * (seatWidth * 0.8 * 0.5),
        z: z + seatHeight * 0.5
      };

      if (!isOnScreen(canvas, camera, seatCenter)) continue; // cull off-screen seats

      // Lighting
      const sunDot = normal.x * sunDir.x + normal.y * sunDir.y;
      const sunOffset = Math.floor(sunDot * sunStrength);
      const tierOffset = -tier * 3;

      const distanceFromPitch = Math.hypot(baseX, baseY);
      const maxDistance = baseA + tiers * tierDepth;
      const radialOffset = Math.floor(
        Math.max(0, 1 - distanceFromPitch / maxDistance) * 20
      );

      const totalOffset = sunOffset + tierOffset + radialOffset;

      const seatColor = seatColors[(i + tier * tier) % seatColors.length];
      const seatBase = shadeColor(seatColor, totalOffset);
      const seatFront = shadeColor(seatColor, totalOffset - 12);

      const manW = seatWidth * 0.3;
      const halfW = seatWidth * 0.5;
      const depth = seatWidth * 0.8;

      // Seat base
      const seatPoly = [
        { x: baseX - tangent.x * halfW, y: baseY - tangent.y * halfW, z },
        { x: baseX + tangent.x * halfW, y: baseY + tangent.y * halfW, z },
        { x: baseX + tangent.x * halfW + normal.x * depth, y: baseY + tangent.y * halfW + normal.y * depth, z },
        { x: baseX - tangent.x * halfW + normal.x * depth, y: baseY - tangent.y * halfW + normal.y * depth, z },
      ];

      fillPoly(ctx, canvas, seatPoly, camera, seatBase);

      // Seat front
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

      // =====================
      // Crowd
      // =====================
      let crowdZ = z + 0.3;
      if (i % 5 <= 2)
        crowdZ += Math.abs(Math.sin(performance.now() / 200 + tier)) * 0.3;

      const crowdCenter = {
        x: baseX + normal.x * depth * 0.5,
        y: baseY + normal.y * depth * 0.5,
        z: crowdZ - 0.2
      };

      if (!isOnScreen(canvas, camera, crowdCenter)) continue; // cull off-screen crowd

      fillPoly(
        ctx,
        canvas,
        [
          { x: baseX - tangent.x * manW + normal.x * depth * 0.5, y: baseY - tangent.y * manW + normal.y * depth * 0.5, z },
          { x: baseX + tangent.x * manW + normal.x * depth * 0.5, y: baseY + tangent.y * manW + normal.y * depth * 0.5, z },
          { x: baseX + tangent.x * manW + normal.x * depth * 0.5, y: baseY + tangent.y * manW + normal.y * depth * 0.5, z: crowdZ },
          { x: baseX - tangent.x * manW + normal.x * depth * 0.5, y: baseY - tangent.y * manW + normal.y * depth * 0.5, z: crowdZ },
        ],
        camera,
        shadeColor(crowdColors[(i + tier) % crowdColors.length], totalOffset + (isNightMatch ? 10 : 0))
      );
    }
  }

  // =====================
  // ROOF (DONUT) with 2D sun
  // =====================
  const roofWallHeight = 2;
  const roofOuterHeight = wallHeight + tiers * tierHeight + roofWallHeight;
  const roofConeHeight = 2;

  const depth = -16;
  const roofOuterA = baseA + tiers * tierDepth;
  const roofOuterB = baseB + tiers * tierDepth;

  drawWalls(ctx, canvas, camera, roofOuterA, roofOuterB, roofOuterHeight - roofWallHeight, roofWallHeight, "#AAAAAA");
  drawCone(ctx, canvas, camera, roofOuterA, roofOuterB, depth, roofOuterHeight, roofConeHeight, "#AAAAAA");
}

function ellipsePoints(a: number, b: number, z: number, steps = 64) {
  const pts = [];
  for (let i = 0; i < steps; i++) {
    const t = (i / steps) * Math.PI * 2;
    pts.push({
      x: a * Math.cos(t),
      y: b * Math.sin(t),
      z,
    });
  }
  return pts;
}

function ellipsePerimeter(a: number, b: number): number {
  const h = Math.pow(a - b, 2) / Math.pow(a + b, 2);
  return Math.PI * (a + b) * (1 + (3 * h) / (10 + Math.sqrt(4 - 3 * h)));
}

function shadeColor(hex: string, amount: number) {
  const num = parseInt(hex.slice(1), 16);
  const r = Math.max(40, Math.min(255, (num >> 16) + amount));
  const g = Math.max(40, Math.min(255, ((num >> 8) & 0xff) + amount));
  const b = Math.max(40, Math.min(255, (num & 0xff) + amount));
  return `rgb(${r},${g},${b})`;
}

function drawWalls(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  camera: Camera,
  a: number,
  b: number,
  z: number,
  wallHeight: number,
  color: string,
) {
  const wallBase = ellipsePoints(a, b, 0);

  for (let i = 0; i < wallBase.length; i++) {
    const c1 = wallBase[i];
    const c2 = wallBase[(i + 1) % wallBase.length];

    const nx = c1.x;
    const ny = c1.y;
    const nLen = Math.hypot(nx, ny);
    const normal = { x: nx / nLen, y: ny / nLen };

    const dot = Math.max(0, normal.x * sunDir.x + normal.y * sunDir.y);
    const horizontalShade = Math.floor(dot * sunStrength) - 20;

    const steps = 10;
    const stepHeight = wallHeight / steps;

    for (let s = 0; s < steps; s++) {
      const verticalOffset = Math.floor((s / steps) * 30);

      const overlap = 0.3;

      const z0 = z + s * stepHeight;
      const z1 = z + (s + 1) * stepHeight + overlap;

      const nx1 = c1.x / Math.hypot(c1.x, c1.y);
      const ny1 = c1.y / Math.hypot(c1.x, c1.y);

      const p3 = { x: c2.x - nx1 * overlap, y: c2.y - ny1 * overlap, z: z1 };
      const p4 = { x: c1.x - nx1 * overlap, y: c1.y - ny1 * overlap, z: z1 };

      const p1 = { x: c1.x, y: c1.y, z: z0 };
      const p2 = { x: c2.x, y: c2.y, z: z0 };

      fillPoly(
        ctx,
        canvas,
        [p1, p2, p3, p4],
        camera,
        shadeColor(color, horizontalShade + verticalOffset)
      );
    }
  }
}

function drawCone(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  camera: Camera,
  a: number,
  b: number,
  depth: number,
  z: number,
  height: number,
  color: string,
) {
  const floorSteps = 32;
  const aInner = a;
  const bInner = b;
  const aOuter = a + depth; // tiers * tierDepth
  const bOuter = b + depth;

  const inner = ellipsePoints(aInner, bInner, z, floorSteps);
  const outer = ellipsePoints(aOuter, bOuter, z + height, floorSteps);

  for (let i = 0; i < floorSteps; i++) {
    const i2 = (i + 1) % floorSteps;

    const nx = (outer[i].x + inner[i].x) * 0.5;
    const ny = (outer[i].y + inner[i].y) * 0.5;
    const nLen = Math.hypot(nx, ny);
    const normal = { x: nx / nLen, y: ny / nLen };

    const dot = Math.max(0, normal.x * sunDir.x + normal.y * sunDir.y); // 2D sun
    const shade = Math.floor(dot * sunStrength);

    fillPoly(
      ctx,
      canvas,
      [
        inner[i],
        inner[i2],
        outer[i2],
        outer[i],
      ],
      camera,
      shadeColor(color, shade)
    );
  }
}

function drawSun(ctx: CanvasRenderingContext2D, canvas: HTMLCanvasElement, camera: Camera) {
  const sunScreen = worldToScreen(canvas, sunWorld, camera);
  if (!sunScreen) return; // behind camera

  const sunRadius = 150;
  const gradient = ctx.createRadialGradient(sunScreen.x, sunScreen.y, 0, sunScreen.x, sunScreen.y, sunRadius);
  gradient.addColorStop(0, "rgba(255, 255, 200, 1)");
  gradient.addColorStop(1, "rgba(255, 255, 200, 0)");

  ctx.fillStyle = gradient;
  ctx.beginPath();
  ctx.arc(sunScreen.x, sunScreen.y, sunRadius, 0, Math.PI * 2);
  ctx.fill();
}