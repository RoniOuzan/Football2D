import { log } from "console";
import React, { useEffect, useRef, useState } from "react";

export const FPS = 30.0;

export interface Translation2d {
  x: number, y: number
}

interface Team {
  players: {
    position: Translation2d,
    velocity: Translation2d,
  }[],
  chosenPlayerIndex: number,
}

interface JsonData {
  ball: {
    position: Translation2d,
    velocity: Translation2d,
  },
  client1: {
    team: Team,
  },
  // client2: {
  //   team: {
  //     players: {
  //       position: Translation2d,
  //       velocity: Translation2d,
  //     }[]
  //   }
  // }
}

function App() {
  const [data, setData] = useState<JsonData>();
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const pressedKeys = useRef<Set<string>>(new Set());

  useEffect(() => {
    const socket = new WebSocket("ws://localhost:8080/game");

    socket.onmessage = (event) => {
      console.log(event.data);
      setData(JSON.parse(event.data));
    };

    const handleKeyDown = (e: KeyboardEvent) => {
      pressedKeys.current.add(e.key.toLowerCase());
    };

    const handleKeyUp = (e: KeyboardEvent) => {
      pressedKeys.current.delete(e.key.toLowerCase());
    };

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

    // Draw field
    ctx.fillStyle = "#006400";
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // Draw center line
    ctx.strokeStyle = "white";
    ctx.beginPath();
    ctx.moveTo(canvas.width / 2, 0);
    ctx.lineTo(canvas.width / 2, canvas.height);
    ctx.stroke();

    // Convert field coordinates to canvas coordinates
    if (data) {
      let ball = convert(canvas, data.ball.position);

      // Draw ball
      ctx.fillStyle = "white";
      ctx.beginPath();
      ctx.arc(ball.x, ball.y, 8, 0, Math.PI * 2);
      ctx.fill();

      console.log(data.client1);
      
      if (data.client1) {
        data.client1.team.players.forEach((p, i) => {
          const pose = convert(canvas, p.position);

          ctx.fillStyle = "red";
          ctx.beginPath();
          ctx.arc(pose.x, pose.y, 12, 0, Math.PI * 2);
          ctx.fill();

          if (i === data.client1.team.chosenPlayerIndex) {
            ctx.strokeStyle = "yellow";
            ctx.lineWidth = 2;
            ctx.beginPath();
            ctx.arc(pose.x, pose.y, 12, 0, Math.PI * 2);
            ctx.stroke();
          }
        });

      }
    }
  }, [data]);

  return (
    <div style={{ textAlign: "center" }}>
      <h1>⚽ Football 2D Simulation</h1>
      <canvas ref={canvasRef} width={800} height={500} />
    </div>
  );
}

const convert = (canvas: HTMLCanvasElement, position: {x: number, y: number}): {x: number, y: number} => {
  return {
    x: (position.x + 50) * (canvas.width / 100),
    y: (25 - position.y) * (canvas.height / 50)
  }
}

export default App;
