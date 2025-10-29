import React, { useEffect, useRef, useState } from "react";

interface JsonData {
  position: {
    x: number,
    y: number,
  };
}

function App() {
  const [data, setData] = useState<JsonData>({ position: { x: 0, y: 0 } });
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const socketRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    // Connect to server
    const socket = new WebSocket("ws://localhost:8080/game");
    socketRef.current = socket;

    socket.onmessage = (event) => {
      setData(JSON.parse(event.data));
      console.log(event.data);
    };

    return () => socket.close();
  }, []);

  // Listen for keyboard input
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!socketRef.current || socketRef.current.readyState !== WebSocket.OPEN) return;

      let dx = 0;
      let dy = 0;

      switch (e.key.toLowerCase()) {
        case "w":
          dy = 1; // up in field coordinates
          break;
        case "s":
          dy = -1; // down
          break;
        case "a":
          dx = -1; // left
          break;
        case "d":
          dx = 1; // right
          break;
      }

      // Send movement to server
      socketRef.current.send(JSON.stringify({ type: "move", dx, dy }));
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  // Draw field and ball
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
    const ballX = (data.position.x + 50) * (canvas.width / 100);
    const ballY = (25 - data.position.y) * (canvas.height / 50);

    // Draw ball
    ctx.fillStyle = "white";
    ctx.beginPath();
    ctx.arc(ballX, ballY, 8, 0, Math.PI * 2);
    ctx.fill();
  }, [data]);

  return (
    <div style={{ textAlign: "center" }}>
      <h1>⚽ Football 2D Simulation</h1>
      <canvas ref={canvasRef} width={800} height={500} />
      <p>Use W A S D to move the ball</p>
    </div>
  );
}

export default App;
