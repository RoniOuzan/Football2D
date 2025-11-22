import { useRef, useEffect, useState } from "react";
import Client from "./Client";
import InputController from "./InputController";
import GameRenderer from "./GameRenderer";
import { JsonData } from "./types";

export const FPS = 30;

export default function Game() {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  const [score, setScore] = useState({ blue: 0, red: 0 });
  const [, setData] = useState<JsonData | null>(null);

  const rendererRef = useRef<GameRenderer | null>(null);
  const client = useRef<Client | null>(null);
  const input = useRef<InputController | null>(null);

  const dataRef = useRef<JsonData | null>(null);

  useEffect(() => {
    if (client.current) return;
    
    client.current = new Client((gameState) => {
      setData(gameState);
      dataRef.current = gameState; // update ref
      setScore({ blue: gameState.score2, red: gameState.score1 });
    }); 

    client.current.connect();

    input.current = new InputController(client.current);
  }, []);

  useEffect(() => {
    rendererRef.current = new GameRenderer();

    const loop = () => {
      if (rendererRef.current && canvasRef.current && dataRef.current) {
        rendererRef.current.draw(canvasRef.current, dataRef.current);
      }
      requestAnimationFrame(loop);
    };
    loop();
  }, []);

  return (
    <div>
      <h2 style={{ color: "#fff" }}>
        <span style={{ color: "blue" }}>{score.blue}</span>
        {"  -  "}
        <span style={{ color: "red" }}>{score.red}</span>
      </h2>

      <canvas ref={canvasRef} width={1080} height={700} />
    </div>
  );
}

