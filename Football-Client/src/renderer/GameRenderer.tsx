import {
  forwardRef,
  useEffect,
  useImperativeHandle,
  useRef,
  useState,
} from "react";
import { isPortrait } from "../App";
import type { Ball, Camera, GameData, Player, ReplayData, Team } from "../client/jsons/gameTypes";
import { getClientsTeam } from "../client/jsons/gameTypes";
import type { Translation2d, Translation3d } from "../types";
import { createRotation2dDeg, createRotation2dRad, maxX, maxY, pitchHeight, pitchWidth } from "../types";
import {
  drawCircleWorld,
  drawCylinder,
  drawLineFlat,
  drawPitch,
  drawRectWorld,
  drawSphere,
  fillPoly,
  strokePoly3d,
} from "./RendererUtil";
import { drawStadium } from "./StadiumRenderer";
import { FPS } from "../game/Game";

interface GameRendererProps {
  data: GameData;
  replay: ReplayData | null;
}

interface Draw {
  pos: Translation3d | Translation2d;
  draw: (ctx: CanvasRenderingContext2D, canvas: HTMLCanvasElement) => void;
}

// eslint-disable-next-line react-refresh/only-export-components
export function radians(a: number) {
  return a * (Math.PI / 180);
}

export interface GameRenderer3DHandle {
  getCanvas: () => HTMLCanvasElement | null;
}

const GameRenderer3D = forwardRef<GameRenderer3DHandle, GameRendererProps>(
  ({ data, replay }, ref) => {
    const canvasRef = useRef<HTMLCanvasElement>(null);

    const [camera, setCamera] = useState<Camera>({
      translation: { x: 0, y: 0, z: 0 },
      yaw: { value: 0, cos: 1, sin: 0 },
      pitch: { value: 0, cos: 1, sin: 0 },
      fov: { value: 90, cos: 1, sin: 0 },
    });

    useImperativeHandle(ref, () => ({
      getCanvas: () => canvasRef.current,
    }));

    useEffect(() => {
      const canvas = canvasRef.current;
      if (!canvas) return;

      const handleResize = () => {
        // Set physical pixels (crucial for Sharpness & Full Screen)
        if (isPortrait) {
          canvas.width = window.innerHeight;
          canvas.height = window.innerWidth;
        } else {
          canvas.width = window.innerWidth;
          canvas.height = window.innerHeight;
        }
        render(); // Re-draw immediately on resize
      };

      const render = () => {
        const ctx = canvas.getContext("2d");
        if (!ctx) return;

        const team = getClientsTeam(data);
        if (replay) {
          setCamera(prev => calculateReplayCamera(data.ball, replay.scoredSide, prev))
        } else {
          if (team)
            setCamera(team.teamStrategy.cameraManager.position);
          else if (data.client <= 0)
            setCamera(data.spectators[-data.client].cameraManager.position);
        }

        // const pitch = radians(-90);
        // const yaw = radians(0);
        // setCamera({
        //   translation: { x: 0, y: 0, z: 150 },
        //   yaw: { value: yaw, cos: Math.cos(yaw), sin: Math.sin(yaw) },
        //   pitch: { value: pitch, cos: Math.cos(pitch), sin: Math.sin(pitch) },
        // })

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

        drawCircleWorld(
          ctx,
          canvas,
          camera,
          { x: 0, y: 0 },
          9.15,
          "white",
          0.25
        );
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

        drawHeatmap(ctx, canvas, team, camera);

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
            getDistanceToCamera(b.pos, camera) -
            getDistanceToCamera(a.pos, camera)
        );
        draws.forEach((d) => d.draw(ctx, canvas));
      };

      handleResize();

      window.addEventListener("resize", handleResize);
      return () => window.removeEventListener("resize", handleResize);
    }, [camera, data]);

    return (
      <canvas
        ref={canvasRef}
        style={{
          width: "100%",
          height: "100%",
          backgroundColor: "#00a6ffff",
          position: "absolute",
          top: 0,
          left: 0,
        }}
      />
    );
  }
);

// =====================
// DRAW HELPERS (SAFE)
// =====================
function getDistanceToCamera(p: Translation3d | Translation2d, c: Camera) {
  const dx = p.x - c.translation.x;
  const dy = p.y - c.translation.y;
  const dz = ("z" in p ? p.z : 0) - c.translation.z;
  return Math.sqrt(dx * dx + dy * dy + dz * dz);
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

function drawHeatmap(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  team: Team | null,
  camera: Camera
) {
  if (!team) return;
  const scoreValues = team.teamStrategy.scores.map((score) => score.value);
  const maxValue = Math.min(Math.max(...scoreValues), 50);
  ctx.save();
  ctx.globalAlpha = 0.3;
  const amount = Math.sqrt(team.teamStrategy.scores.length);
  const sizeX = pitchWidth / amount;
  const sizeY = pitchHeight / amount;
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

export function calculateReplayCamera(
  ball: Ball,
  scoreSide: number,
  prevCam: Camera,
  dt = 1 / FPS
): Camera {
  const goal = getGoal(scoreSide);

  // Step 1: Predict future ball
  const predictionTime = 0.3;
  const futureBall: Translation2d = {
    x: ball.position.x + ball.velocity.x * predictionTime,
    y: ball.position.y + ball.velocity.y * predictionTime,
  };

  // Step 2: Compute cinematic offsets
  const nearGoalThreshold = 20;
  const ballSpeed = Math.hypot(ball.velocity.x, ball.velocity.y);
  const distanceToGoal = Math.abs(goal.x - ball.position.x);

  let offsetX = 0;
  let offsetY = 0;
  let offsetZ = 15;

  if (distanceToGoal < nearGoalThreshold || ballSpeed > 6) {
    // Goal attempt: pull camera back and to side
    const side = Math.sign(ball.velocity.y) || 1;
    offsetX = -side * 18; // orbit from side
    offsetY = -20;         // pull back
    offsetZ = 18;          // raise camera
  } else {
    // Normal play: slightly behind the ball
    offsetX = -10; // behind ball along x
    offsetY = -10; // behind along y
    offsetZ = 15;
  }

  // Step 3: Compute desired camera position
  let desiredCam: Translation3d = {
    x: futureBall.x + offsetX,
    y: futureBall.y + offsetY,
    z: offsetZ,
  };

  // Smooth camera translation
  desiredCam = {
    x: prevCam.translation.x + (desiredCam.x - prevCam.translation.x) * 0.1,
    y: prevCam.translation.y + (desiredCam.y - prevCam.translation.y) * 0.1,
    z: prevCam.translation.z + (desiredCam.z - prevCam.translation.z) * 0.1,
  };

  const targetFocus: Translation2d =
  distanceToGoal < nearGoalThreshold ? goal : futureBall;

  const dx = targetFocus.x - desiredCam.x;
  const dy = targetFocus.y - desiredCam.y;
  const targetZ = 1.5; // ball height
  const dz = targetZ - desiredCam.z;

  const yaw = Math.atan2(dy, dx) - Math.PI / 2; // yaw 0 = +y
  const pitch = Math.atan2(dz, Math.hypot(dx, dy)); // look toward target

  // Smooth
  const smoothYaw = smoothRotation(prevCam.yaw.value, yaw, dt, 5);
  const smoothPitch = smoothRotation(prevCam.pitch.value, pitch, dt, 5);

  return {
    translation: desiredCam,
    yaw: createRotation2dRad(smoothYaw),
    pitch: createRotation2dRad(smoothPitch),
    fov: createRotation2dDeg(40),
  };
}

function getGoal(side: number): Translation2d {
  if (side > 0) {
    return { x: -maxX, y: maxY};
  } else if (side < 0)
    return { x: maxX, y: maxY};
  
  return { x: 0, y: 0};
}

// Helper to smooth rotation angles
function smoothRotation(prev: number, target: number, dt: number, speed: number) {
  let diff = target - prev;
  while (diff > Math.PI) diff -= 2 * Math.PI;
  while (diff < -Math.PI) diff += 2 * Math.PI;
  return prev + diff * dt * speed;
}

export default GameRenderer3D;
