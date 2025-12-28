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