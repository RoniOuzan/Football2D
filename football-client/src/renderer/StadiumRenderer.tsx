import { Camera, maxX, maxY } from "../types";
import { fillPoly } from "./RendererUtil";

export function drawStadium(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  camera: Camera
) {
  const wallHeight = 2;
  const standDepth = 18;

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
  // LIGHTING
  // =====================
  const isNightMatch = false;
  const sunDir = { x: -0.6, y: 0.8 };
  const sunStrength = isNightMatch ? 0 : 30;

  function shadeColor(hex: string, amount: number) {
    const num = parseInt(hex.slice(1), 16);
    const r = Math.max(40, Math.min(255, (num >> 16) + amount));
    const g = Math.max(40, Math.min(255, ((num >> 8) & 0xff) + amount));
    const b = Math.max(40, Math.min(255, (num & 0xff) + amount));
    return `rgb(${r},${g},${b})`;
  }

  // =====================
  // ELLIPSE HELPERS
  // =====================
  const baseA = maxX + standDepth;
  const baseB = maxY + standDepth;

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

  function ellipseAtTier(tier: number) {
    const r = tier * tierDepth;
    return {
      a: baseA + r,
      b: baseB + r,
    };
  }

  function ellipsePerimeter(a: number, b: number): number {
    const h = Math.pow(a - b, 2) / Math.pow(a + b, 2);
    return Math.PI * (a + b) * (1 + (3 * h) / (10 + Math.sqrt(4 - 3 * h)));
  }

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
  const floorSteps = 16;
  const aInner = baseA;
  const bInner = baseB;
  const aOuter = baseA + tiers * tierDepth;
  const bOuter = baseB + tiers * tierDepth;

  const inner = ellipsePoints(aInner, bInner, wallHeight - 0.5, floorSteps);
  const outer = ellipsePoints(aOuter, bOuter, wallHeight - 0.5, floorSteps);

  for (let i = 0; i < floorSteps; i++) {
    const i2 = (i + 1) % floorSteps;

    const zOuter = wallHeight + tiers * tierHeight; // gentle ramp

    const nLen = Math.hypot(inner[i].x, inner[i].y);
    const normal = { x: inner[i].x / nLen, y: inner[i].y / nLen };
    const dot = normal.x * sunDir.x + normal.y * sunDir.y;

    const shade = Math.floor(dot * sunStrength) - 30;

    fillPoly(
      ctx,
      canvas,
      [
        inner[i],
        inner[i2],
        { ...outer[i2], z: zOuter },
        { ...outer[i], z: zOuter },
      ],
      camera,
      shadeColor(standColor, shade)
    );
  }

  // =====================
  // WALLS (ELLIPSE EXTRUSION)
  // =====================
  const wallBase = ellipsePoints(baseA, baseB, 0);

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

      const z0 = s * stepHeight;
      const z1 = (s + 1) * stepHeight + overlap;

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
        shadeColor(wallColor, horizontalShade + verticalOffset)
      );
    }
  }

  // =====================
  // STANDS + SEATS (ELLIPSE)
  // =====================
  for (let tier = 0; tier < tiers; tier++) {
    const z = wallHeight + 0.2 + tier * tierHeight;
    const { a, b } = ellipseAtTier(tier);
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

      const seatColor = seatColors[(i + tier * 2) % seatColors.length];
      const seatBase = shadeColor(seatColor, totalOffset);
      const seatFront = shadeColor(seatColor, totalOffset - 12);

      const manW = seatWidth * 0.3;
      const halfW = seatWidth * 0.5;
      const depth = seatWidth * 0.8;

      const seatPoly = [
        { x: baseX - tangent.x * halfW, y: baseY - tangent.y * halfW, z },
        { x: baseX + tangent.x * halfW, y: baseY + tangent.y * halfW, z },
        {
          x: baseX + tangent.x * halfW + normal.x * depth,
          y: baseY + tangent.y * halfW + normal.y * depth,
          z,
        },
        {
          x: baseX - tangent.x * halfW + normal.x * depth,
          y: baseY - tangent.y * halfW + normal.y * depth,
          z,
        },
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

      // =====================
      // CROWD
      // =====================
      const crowdZ =
        z + 0.3 + Math.abs(Math.sin(performance.now() / 200 + tier)) * 0.3;

      fillPoly(
        ctx,
        canvas,
        [
          {
            x: baseX - tangent.x * manW + normal.x * depth * 0.5,
            y: baseY - tangent.y * manW + normal.y * depth * 0.5,
            z,
          },
          {
            x: baseX + tangent.x * manW + normal.x * depth * 0.5,
            y: baseY + tangent.y * manW + normal.y * depth * 0.5,
            z,
          },
          {
            x: baseX + tangent.x * manW + normal.x * depth * 0.5,
            y: baseY + tangent.y * manW + normal.y * depth * 0.5,
            z: crowdZ,
          },
          {
            x: baseX - tangent.x * manW + normal.x * depth * 0.5,
            y: baseY - tangent.y * manW + normal.y * depth * 0.5,
            z: crowdZ,
          },
        ],
        camera,
        shadeColor(
          crowdColors[(i + tier) % crowdColors.length],
          totalOffset + (isNightMatch ? 10 : 0)
        )
      );
    }
  }

  // =====================
  // ROOF (DONUT) with 2D sun
  // =====================
  const roofOuterHeight = wallHeight + tiers * tierHeight + 2;
  const roofInnerHeight = roofOuterHeight + 2;
  const roofSteps = 64;

  const roofInnerA = baseA - 12;
  const roofInnerB = baseB - 12;
  const roofOuterA = baseA + tiers * tierDepth;
  const roofOuterB = baseB + tiers * tierDepth;

  const roofInner = ellipsePoints(
    roofInnerA,
    roofInnerB,
    roofInnerHeight,
    roofSteps
  );
  const roofOuter = ellipsePoints(
    roofOuterA,
    roofOuterB,
    roofOuterHeight,
    roofSteps
  );

  for (let i = 0; i < roofSteps; i++) {
    const i2 = (i + 1) % roofSteps;

    // horizontal normal for shading
    const nx = (roofOuter[i].x + roofInner[i].x) * 0.5;
    const ny = (roofOuter[i].y + roofInner[i].y) * 0.5;
    const nLen = Math.hypot(nx, ny);
    const normal = { x: nx / nLen, y: ny / nLen };

    const dot = Math.max(0, normal.x * sunDir.x + normal.y * sunDir.y); // 2D sun
    const shade = Math.floor(dot * sunStrength);

    fillPoly(
      ctx,
      canvas,
      [roofInner[i], roofInner[i2], roofOuter[i2], roofOuter[i]],
      camera,
      shadeColor("#bbbbbb", shade)
    );
  }
}