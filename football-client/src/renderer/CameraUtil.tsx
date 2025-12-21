import { Translation3d, Translation2d, Camera } from "../types";

const ZOOM = 1;
const NEAR = 0.05;

type CamPoint = { x: number; y: number; z: number, fov: number };

// =====================
// CAMERA SPACE
// =====================
export function worldToCamera(
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
    fov: camera.fov.value
  };
}

export function cameraToScreen(canvas: HTMLCanvasElement, p: CamPoint): Translation2d {
  const focal = 1 / Math.tan(p.fov / 2);
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
export function worldToScreen(
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
export function intersect(A: CamPoint, B: CamPoint): CamPoint {
  const t = (NEAR - A.y) / (B.y - A.y);
  return {
    x: A.x + t * (B.x - A.x),
    z: A.z + t * (B.z - A.z),
    y: NEAR,
    fov: A.fov
  };
}

export function clipNearPlane(points: CamPoint[]): CamPoint[] {
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

export function isOnScreen(canvas: HTMLCanvasElement, camera: Camera, pos: Translation3d) {
  const screen = worldToScreen(canvas, pos, camera);
  if (!screen) return false;
  const margin = canvas.width * 0.05;
  return screen.x >= -margin && screen.x <= canvas.width + margin && screen.y >= -margin && screen.y <= canvas.height + margin;
}