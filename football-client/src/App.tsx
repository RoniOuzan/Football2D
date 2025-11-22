import { log } from "console";
import React, { useEffect, useRef, useState } from "react";

export const FPS = 30.0;
export const pitchWidthUnits = 100;
export const pitchHeightUnits = 64;

export interface Translation2d {
  x: number;
  y: number;
}

export interface JsonData {
  ball: Ball;
  team1: Team;
  team2: Team;
  score1: number;
  score2: number;
}

export interface Ball {
  position: Translation2d;
  velocity: Translation2d;
}

export interface Team {
  players: Player[];
  teamStrategy: TeamStrategy;
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
  defenseLine: number;
  chosenPlayerIndex: number;
}

function App() {
  const [data, setData] = useState<JsonData>();
  const [score1, setScore1] = useState(0);
  const [score2, setScore2] = useState(0);
  const [flash, setFlash] = useState(false);
  const [goalText, setGoalText] = useState("");

  const canvasRef = useRef<HTMLCanvasElement>(null);
  const pressedKeys = useRef<Set<string>>(new Set());

  // WebSocket connection
  useEffect(() => {
    const socket = new WebSocket("ws://localhost:9090/game");

    socket.onmessage = (event) => {
      if (!event.data) return;
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

  // Detect goals and trigger flash
  useEffect(() => {
    if (!data) return;

    if (data.score1 !== score1) {
      setFlash(true);
      setGoalText("GOAL RED!");
      setTimeout(() => setFlash(false), 500);
      setTimeout(() => setGoalText(""), 800);
      setScore1(data.score1);
    }

    if (data.score2 !== score2) {
      setFlash(true);
      setGoalText("GOAL BLUE!");
      setTimeout(() => setFlash(false), 500);
      setTimeout(() => setGoalText(""), 800);
      setScore2(data.score2);
    }
  }, [data]);

  // Draw pitch, players, ball, scores
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d")!;
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    const playerRadius = 0.75 * (canvas.height / 64);

    // --- Pitch dimensions ---
    const pitchWidth = canvas.width;
    const pitchHeight = canvas.height;
    const goalDepth = (3 / pitchWidthUnits) * pitchWidth;
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
    const goalWidth = (7.3 / pitchHeightUnits) * pitchHeight;
    const goalPostRadius = (0.35 / pitchHeightUnits) * pitchHeight;
    const goalTop = (pitchHeight - goalWidth) / 2;
    const netLines = 6;

    // LEFT GOAL
    ctx.strokeStyle = "red";
    ctx.lineWidth = 4;

    ctx.beginPath();
    ctx.moveTo(pitchLeft - goalDepth, goalTop);
    ctx.lineTo(pitchLeft - goalDepth, goalTop + goalWidth);
    ctx.lineTo(pitchLeft, goalTop + goalWidth);
    ctx.lineTo(pitchLeft, goalTop);
    ctx.lineTo(pitchLeft - goalDepth, goalTop);
    ctx.stroke();

    ctx.lineWidth = 1;
    ctx.beginPath();
    for (let i = 1; i < netLines; i++) {
      const x = pitchLeft - (goalDepth * i) / netLines;
      ctx.moveTo(x, goalTop);
      ctx.lineTo(x, goalTop + goalWidth);
    }
    ctx.stroke();

    ctx.beginPath();
    for (let i = 1; i < netLines; i++) {
      const y = goalTop + (goalWidth * i) / netLines;
      ctx.moveTo(pitchLeft - goalDepth, y);
      ctx.lineTo(pitchLeft, y);
    }
    ctx.stroke();

    // RIGHT GOAL
    ctx.strokeStyle = "blue";
    ctx.lineWidth = 4;

    ctx.beginPath();
    ctx.moveTo(pitchRight + goalDepth, goalTop);
    ctx.lineTo(pitchRight + goalDepth, goalTop + goalWidth);
    ctx.lineTo(pitchRight, goalTop + goalWidth);
    ctx.lineTo(pitchRight, goalTop);
    ctx.lineTo(pitchRight + goalDepth, goalTop);
    ctx.stroke();

    ctx.lineWidth = 1;
    ctx.beginPath();
    for (let i = 1; i < netLines; i++) {
      const x = pitchRight + (goalDepth * i) / netLines;
      ctx.moveTo(x, goalTop);
      ctx.lineTo(x, goalTop + goalWidth);
    }
    ctx.stroke();

    ctx.beginPath();
    for (let i = 1; i < netLines; i++) {
      const y = goalTop + (goalWidth * i) / netLines;
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
      if (data.team1) {
        data.team1.players.forEach((p, i) => {
          const pose = convert(canvas, p.position, goalDepth);
          ctx.fillStyle = i === 0 ? "#FF5555" : "#FF0000";
          ctx.beginPath();
          ctx.arc(pose.x, pose.y, playerRadius, 0, Math.PI * 2);
          ctx.fill();

          if (i === data.team1.teamStrategy.chosenPlayerIndex) {
            ctx.strokeStyle = "yellow";
            ctx.lineWidth = 2;
            ctx.beginPath();
            ctx.arc(pose.x, pose.y, playerRadius, 0, Math.PI * 2);
            ctx.stroke();
          }
        });
      }

      if (data.team2) {
        data.team2.players.forEach((p, i) => {
          const pose = convert(canvas, p.position, goalDepth);
          ctx.fillStyle = i === 0 ? "#5555FF" : "#0000FF";
          ctx.beginPath();
          ctx.arc(pose.x, pose.y, playerRadius, 0, Math.PI * 2);
          ctx.fill();

          if (i === data.team2.teamStrategy.chosenPlayerIndex) {
            ctx.strokeStyle = "yellow";
            ctx.lineWidth = 2;
            ctx.beginPath();
            ctx.arc(pose.x, pose.y, playerRadius, 0, Math.PI * 2);
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
          const color = scoreToColor(entry.value, -120, Math.min(max, 50));

          // Draw transparent rectangle
          ctx.fillStyle = color;
          ctx.globalAlpha = 0.3;
          const sizeX = (canvas.width - 2 * goalDepth) / 40;
          const sizeY = canvas.height / 40;
          ctx.fillRect(pos.x - sizeX / 2, pos.y - sizeY / 2, sizeX, sizeY);

          // Draw the score text
          // ctx.fillStyle = "black"; // or white if better contrast
          // ctx.font = "12px Arial";
          // ctx.textAlign = "center";
          // ctx.textBaseline = "middle";
          // ctx.fillText(entry.value.toFixed(1), pos.x, pos.y);
          ctx.globalAlpha = 1.0;
        });
      };


      drawScores(data.team1);
      // drawScores(data.client2?.team);

      const defenseX_units = data.team1.teamStrategy.defenseLine;

      // Convert X (in field units) to canvas pixels
      const pitchWidthUnits = 100;
      const pitchLeft = goalDepth;
      const pitchRight = canvas.width - goalDepth;

      const defenseX = pitchLeft + ((defenseX_units + 50) / pitchWidthUnits) * (pitchRight - pitchLeft);

      ctx.strokeStyle = "yellow";
      ctx.lineWidth = 2;

      ctx.beginPath();
      ctx.moveTo(defenseX, 0);
      ctx.lineTo(defenseX, canvas.height);
      ctx.stroke();
    }
  }, [data]);

  return (
    <div style={{ textAlign: "center" }}>
      <style>
        {`
          @keyframes fadeOut {
            0% { opacity: 1; transform: scale(1); }
            100% { opacity: 0; transform: scale(1.3); }
          }

          @keyframes shake {
            0%, 100% { transform: translateX(0); }
            20% { transform: translateX(-5px); }
            40% { transform: translateX(5px); }
            60% { transform: translateX(-5px); }
            80% { transform: translateX(5px); }
          }
        `}
      </style>

      <h1>⚽ Football 2D</h1>

      {/* GOAL POPUP */}
      {goalText && (
        <div
          style={{
            color: "yellow",
            fontSize: "40px",
            fontWeight: "bold",
            marginBottom: "10px",
            animation: "fadeOut 0.8s forwards",
          }}
        >
          {goalText}
        </div>
      )}

      {/* SCOREBOARD */}
      <div
        style={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          gap: "30px",
          padding: "10px 30px",
          marginBottom: "15px",
          borderRadius: "12px",
          background: flash ? "#ffd700" : "#222",
          border: "3px solid #444",
          color: "white",
          fontSize: "32px",
          fontWeight: "bold",
          width: "fit-content",
          marginLeft: "auto",
          marginRight: "auto",
          boxShadow: flash ? "0 0 25px #ffd700" : "0 0 20px rgba(0,0,0,0.3)",
          transition: "background 0.2s, box-shadow 0.2s",
          animation: flash ? "shake 0.3s" : "",
        }}
      >
        <span style={{ color: "red", fontSize: flash ? "42px" : "32px", transition: "0.2s" }}>
          {score1}
        </span>
        <span style={{ color: "white" }}> : </span>
        <span style={{ color: "blue", fontSize: flash ? "42px" : "32px", transition: "0.2s" }}>
          {score2}
        </span>
      </div>

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
