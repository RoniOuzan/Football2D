import { log } from "console";
import React, { useEffect, useRef, useState } from "react";

export const FPS = 30.0;

export interface Translation2d {
  x: number;
  y: number;
}

export interface JsonData {
  ball: Ball;
  client1: Client;
  client2: Client;
}

export interface Ball {
  position: Translation2d;
  velocity: Translation2d;
}

export interface Client {
  team: Team;
}

export interface Team {
  players: Player[];
  teamStrategy: TeamStrategy;
  chosenPlayerIndex: number;
}

export interface Player {
  position: Translation2d;
  originalPosition: Translation2d;
  direction: Direction;
  velocity: Translation2d;
  targetVelocity: Translation2d;
}

export interface Direction {
  value: number;
  cos: number;
  sin: number;
}

export interface TeamStrategy {
  scores: {
    key: Translation2d;
    value: number;
  }[];
}

function App() {
  const [data, setData] = useState<JsonData>();
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const pressedKeys = useRef<Set<string>>(new Set());

  useEffect(() => {
    const socket = new WebSocket("ws://localhost:8080/game");

    socket.onmessage = (event) => {
      // console.log(event.data);
      
      setData(JSON.parse(event.data));
    };

    const handleKeyDown = (e: KeyboardEvent) =>
      pressedKeys.current.add(e.key.toLowerCase());
    const handleKeyUp = (e: KeyboardEvent) =>
      pressedKeys.current.delete(e.key.toLowerCase());

    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("keyup", handleKeyUp);

    const sendInterval = setInterval(() => {
      if (socket.readyState === WebSocket.OPEN) {
        socket.send(
          JSON.stringify({
            type: "input",
            keys: Array.from(pressedKeys.current),
          })
        );
      }
    }, 1000 / FPS);

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("keyup", handleKeyUp);
      clearInterval(sendInterval);
      socket.close();
    };
  }, []);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d")!;
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // console.log(data);

    // --- Pitch dimensions ---
    const pitchWidthUnits = 100;
    const pitchHeightUnits = 64;
    const pitchWidth = canvas.width;
    const pitchHeight = canvas.height;

    // Goal depth in units
    const goalDepthUnits = 3;
    const goalDepth = (goalDepthUnits / pitchWidthUnits) * pitchWidth;

    const pitchLeft = goalDepth;
    const pitchRight = pitchWidth - goalDepth;

    ctx.lineWidth = 2;
    ctx.strokeStyle = "white";

    // --- Pitch background ---
    ctx.fillStyle = "#006400";
    ctx.fillRect(pitchLeft, 0, pitchRight - pitchLeft, pitchHeight);

    // --- Outer boundary ---
    ctx.strokeRect(pitchLeft, 0, pitchRight - pitchLeft, pitchHeight);

    // --- Center line ---
    ctx.beginPath();
    ctx.moveTo(pitchWidth / 2, 0);
    ctx.lineTo(pitchWidth / 2, pitchHeight);
    ctx.stroke();

    // --- Center circle ---
    const centerCircleRadius = (10 / pitchHeightUnits) * pitchHeight;
    ctx.beginPath();
    ctx.arc(pitchWidth / 2, pitchHeight / 2, centerCircleRadius, 0, Math.PI * 2);
    ctx.stroke();

    ctx.fillStyle = "white";
    ctx.beginPath();
    ctx.arc(pitchWidth / 2, pitchHeight / 2, 4, 0, Math.PI * 2);
    ctx.fill();

    // --- Penalty areas ---
    const penaltyWidth = (16.5 / pitchWidthUnits) * pitchWidth;
    const penaltyHeight = (40.3 / pitchHeightUnits) * pitchHeight;
    const penaltyYOffset = (pitchHeight - penaltyHeight) / 2;

    ctx.strokeRect(pitchLeft, penaltyYOffset, penaltyWidth, penaltyHeight);
    ctx.strokeRect(pitchRight - penaltyWidth, penaltyYOffset, penaltyWidth, penaltyHeight);

    // --- 6-yard boxes ---
    const sixYardWidth = (5.5 / pitchWidthUnits) * pitchWidth;
    const sixYardHeight = (18.3 / pitchHeightUnits) * pitchHeight;
    const sixYardYOffset = (pitchHeight - sixYardHeight) / 2;

    ctx.strokeRect(pitchLeft, sixYardYOffset, sixYardWidth, sixYardHeight);
    ctx.strokeRect(pitchRight - sixYardWidth, sixYardYOffset, sixYardWidth, sixYardHeight);

    // --- Penalty spots ---
    const penaltySpotOffset = (11 / pitchWidthUnits) * pitchWidth;
    ctx.fillStyle = "white";

    ctx.beginPath();
    ctx.arc(pitchLeft + penaltySpotOffset, pitchHeight / 2, 3, 0, Math.PI * 2);
    ctx.fill();

    ctx.beginPath();
    ctx.arc(pitchRight - penaltySpotOffset, pitchHeight / 2, 3, 0, Math.PI * 2);
    ctx.fill();

    // --- Penalty arcs ---
    const penaltyArcRadius = (9.15 / pitchHeightUnits) * pitchHeight;

    ctx.beginPath();
    ctx.arc(
      pitchLeft + penaltySpotOffset,
      pitchHeight / 2,
      penaltyArcRadius,
      1.7 * Math.PI,
      0.3 * Math.PI
    );
    ctx.stroke();

    ctx.beginPath();
    ctx.arc(
      pitchRight - penaltySpotOffset,
      pitchHeight / 2,
      penaltyArcRadius,
      1.3 * Math.PI,
      0.7 * Math.PI,
      true
    );
    ctx.stroke();

    // --- Corner arcs ---
    const cornerRadius = (1.2 / pitchHeightUnits) * pitchHeight;

    ctx.beginPath();
    ctx.arc(pitchLeft, 0, cornerRadius, 0, Math.PI / 2);
    ctx.stroke();
    ctx.beginPath();
    ctx.arc(pitchLeft, pitchHeight, cornerRadius, 3 * Math.PI / 2, 2 * Math.PI);
    ctx.stroke();
    ctx.beginPath();
    ctx.arc(pitchRight, 0, cornerRadius, Math.PI / 2, Math.PI);
    ctx.stroke();
    ctx.beginPath();
    ctx.arc(pitchRight, pitchHeight, cornerRadius, Math.PI, 3 * Math.PI / 2);
    ctx.stroke();

    // --- Goals as posts with crossbar + net effect ---
    const goalHeight = (7.3 / pitchHeightUnits) * pitchHeight; // 2.44 units
    const goalTop = (pitchHeight - goalHeight) / 2;
    const netLines = 6; // Number of vertical/horizontal lines to simulate net

    // LEFT GOAL
    ctx.strokeStyle = "red";
    ctx.lineWidth = 4;

    // Goal rectangle (posts + crossbar)
    ctx.beginPath();
    ctx.moveTo(pitchLeft - goalDepth, goalTop); // back top-left
    ctx.lineTo(pitchLeft - goalDepth, goalTop + goalHeight); // back bottom-left
    ctx.lineTo(pitchLeft, goalTop + goalHeight); // front bottom-left
    ctx.lineTo(pitchLeft, goalTop); // front top-left
    ctx.lineTo(pitchLeft - goalDepth, goalTop); // back top-left
    ctx.stroke();

    // Net effect (vertical lines)
    ctx.lineWidth = 1;
    ctx.beginPath();
    for (let i = 1; i < netLines; i++) {
      const x = pitchLeft - (goalDepth * i) / netLines;
      ctx.moveTo(x, goalTop);
      ctx.lineTo(x, goalTop + goalHeight);
    }
    ctx.stroke();

    // Net effect (horizontal lines)
    ctx.beginPath();
    for (let i = 1; i < netLines; i++) {
      const y = goalTop + (goalHeight * i) / netLines;
      ctx.moveTo(pitchLeft - goalDepth, y);
      ctx.lineTo(pitchLeft, y);
    }
    ctx.stroke();

    // RIGHT GOAL
    ctx.strokeStyle = "blue";
    ctx.lineWidth = 4;

    // Goal rectangle
    ctx.beginPath();
    ctx.moveTo(pitchRight + goalDepth, goalTop); // back top-right
    ctx.lineTo(pitchRight + goalDepth, goalTop + goalHeight); // back bottom-right
    ctx.lineTo(pitchRight, goalTop + goalHeight); // front bottom-right
    ctx.lineTo(pitchRight, goalTop); // front top-right
    ctx.lineTo(pitchRight + goalDepth, goalTop); // back top-right
    ctx.stroke();

    // Net effect (vertical lines)
    ctx.lineWidth = 1;
    ctx.beginPath();
    for (let i = 1; i < netLines; i++) {
      const x = pitchRight + (goalDepth * i) / netLines;
      ctx.moveTo(x, goalTop);
      ctx.lineTo(x, goalTop + goalHeight);
    }
    ctx.stroke();

    // Net effect (horizontal lines)
    ctx.beginPath();
    for (let i = 1; i < netLines; i++) {
      const y = goalTop + (goalHeight * i) / netLines;
      ctx.moveTo(pitchRight + goalDepth, y);
      ctx.lineTo(pitchRight, y);
    }
    ctx.stroke();

    // --- Ball ---
    if (data) {
      const ball = convert(canvas, data.ball.position, goalDepth);
      ctx.fillStyle = "white";
      ctx.beginPath();
      ctx.arc(ball.x, ball.y, 5, 0, Math.PI * 2);
      ctx.fill();

      // --- Players ---
      if (data.client1) {
        data.client1.team.players.forEach((p, i) => {
          const pose = convert(canvas, p.position, goalDepth);
          ctx.fillStyle = "red";
          ctx.beginPath();
          ctx.arc(pose.x, pose.y, 8, 0, Math.PI * 2);
          ctx.fill();

          if (i === data.client1.team.chosenPlayerIndex) {
            ctx.strokeStyle = "yellow";
            ctx.lineWidth = 2;
            ctx.beginPath();
            ctx.arc(pose.x, pose.y, 8, 0, Math.PI * 2);
            ctx.stroke();
          }
        });
      }

      if (data.client2) {
        data.client2.team.players.forEach((p, i) => {
          const pose = convert(canvas, p.position, goalDepth);
          ctx.fillStyle = "blue";
          ctx.beginPath();
          ctx.arc(pose.x, pose.y, 8, 0, Math.PI * 2);
          ctx.fill();

          if (i === data.client2.team.chosenPlayerIndex) {
            ctx.strokeStyle = "yellow";
            ctx.lineWidth = 2;
            ctx.beginPath();
            ctx.arc(pose.x, pose.y, 5, 0, Math.PI * 2);
            ctx.stroke();
          }
        });
      }

      const drawScores = (team: Team | undefined) => {
        if (!team) return;
        const scores = team.teamStrategy.scores;
        if (!scores || scores.length === 0) return;

        const values = scores.map(s => s.value);
        const min = Math.min(...values);
        const max = Math.max(...values);

        scores.forEach((entry) => {
          const pos = convert(canvas, entry.key, goalDepth);
          const color = scoreToColor(entry.value, -320, Math.min(max, 50));

          // Draw transparent rectangle
          ctx.fillStyle = color;
          ctx.globalAlpha = 0.3;
          const sizeX = (canvas.width - 2 * goalDepth) / 25;
          const sizeY = canvas.height / 25;
          ctx.fillRect(pos.x - sizeX / 2, pos.y - sizeY / 2, sizeX, sizeY);
          ctx.globalAlpha = 1.0;

          // Draw the score text
          ctx.fillStyle = "black"; // or white if better contrast
          ctx.font = "12px Arial";
          ctx.textAlign = "center";
          ctx.textBaseline = "middle";
          ctx.fillText(entry.value.toFixed(1), pos.x, pos.y);
        });
      };


      drawScores(data.client1?.team);
      drawScores(data.client2?.team);
    }
  }, [data]);

  return (
    <div style={{ textAlign: "center" }}>
      <h1>⚽ Football 2D</h1>
      <canvas ref={canvasRef} width={1080} height={700} />
    </div>
  );
}

// Converts game units to canvas coordinates
const convert = (
  canvas: HTMLCanvasElement,
  position: { x: number; y: number },
  goalDepth: number
) => {
  const pitchWidthUnits = 100;
  const pitchHeightUnits = 64;

  const pitchLeft = goalDepth;
  const pitchRight = canvas.width - goalDepth;

  return {
    x: pitchLeft + ((position.x + 50) / pitchWidthUnits) * (pitchRight - pitchLeft),
    y: ((32 - position.y) / pitchHeightUnits) * canvas.height,
  };
};

function scoreToColor(value: number, min: number, max: number): string {
  // Avoid divide-by-zero
  if (max === min) return "white";

  const t = (value - min) / (max - min); // normalized 0..1

  // Interpolate from blue → green → yellow → red
  const r = Math.floor(255 * t);
  const g = Math.floor(255 * (1 - Math.abs(t - 0.5) * 2));
  const b = Math.floor(255 * (1 - t));

  return `rgb(${r}, ${g}, ${b})`;
}

export default App;
