import { useRef, useEffect, useState } from "react";
import Client from "./Client";
import InputController from "./InputController";
import GameRenderer from "./GameRenderer";
import { JsonData } from "./types";

export const FPS = 30;

export default function Game() {
  const [score, setScore] = useState({ blue: 0, red: 0 });
  const [, setData] = useState<JsonData | null>(null);

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

  return (
    <div>
      <div style={{ 
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        gap: "30px",
        padding: "10px 30px",
        marginBottom: "15px",
        borderRadius: "12px",
        // background: "#222",
        // border: "3px solid #444",
        color: "white",
        fontSize: "42px",
        fontWeight: "bold",
        width: "fit-content",
        marginLeft: "auto",
        marginRight: "auto",
        boxShadow: "0 0 20px rgba(0,0,0,0.3)",
        transition: "background 0.2s, box-shadow 0.2s",
        animation: "",
       }}>
        <span style={{ color: "blue" }}>{score.blue}</span>
        {"  -  "}
        <span style={{ color: "red" }}>{score.red}</span>
      </div>

      {dataRef.current && <GameRenderer data={dataRef.current}/>}
    </div>
  );
}

