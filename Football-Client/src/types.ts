export const pitchWidth = 100;
export const pitchHeight = 64;
export const maxX = pitchWidth / 2;
export const maxY = pitchHeight / 2;

export interface Translation2d {
  x: number;
  y: number;
}

export interface Translation3d {
  x: number;
  y: number;
  z: number;
}

export interface Rotation2d {
  value: number;
  cos: number;
  sin: number;
}

export function createRotation2dDeg(degrees: number): Rotation2d {
  const radians = (degrees * Math.PI) / 180;
  return createRotation2dRad(radians);
}

export function createRotation2dRad(radians: number): Rotation2d {
  return {
    value: radians,
    cos: Math.cos(radians),
    sin: Math.sin(radians),
  };
}