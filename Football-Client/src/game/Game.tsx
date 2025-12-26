import { useRef, useEffect, useState } from "react";
import Client from "../Client";
import InputController from "../InputController";
import { getClientsTeam, type JsonData } from "../types";
import GameRenderer3D, { type GameRenderer3DHandle } from "../renderer/GameRenderer";
import MobileControls from "./MobileControls";
import { isMobile } from "../App";
import { screenToWorld } from "../renderer/CameraUtil";

export const FPS = 30;

export default function Game() {
  const [score] = useState({ blue: 0, red: 0 });
  const [data, setData] = useState<JsonData | null>(null);

  const client = useRef<Client | null>(null);
  const input = useRef<InputController | null>(null);
  const rendererRef = useRef<GameRenderer3DHandle>(null);

  useEffect(() => {
    if (client.current) return;

    client.current = new Client((gameState) => {
      setData(gameState);
    });

    setTimeout(() => {
      client.current?.connect();
    }, 0);

    input.current = new InputController(client.current);

    return () => {
      // optional cleanup later
    };
  }, []);

  function handleTap(e: React.MouseEvent, pressed: boolean) {
    if (!data || !rendererRef.current || !input.current) return;

    const canvas = rendererRef.current.getCanvas();
    if (!canvas) return;

    if (!pressed) {
      input.current.setClick(null);
      return;
    }
    const camera = getClientsTeam(data).teamStrategy.cameraManager.position;
    const world = screenToWorld(canvas, {x: e.clientX, y: e.clientY}, camera);
    input.current.setClick(world);
  }

  return (
    <div
      style={{
        margin: "0",
        padding: "0",
        width: "100%",
        height: "100%",
        position: "fixed",
        overflow: "hidden",
        overscrollBehavior: "none",
      }}
    >
      {isMobile && (
        <MobileControls
          onMove={(x, y) => input.current?.setJoystick(x, y)}
          onStop={() => input.current?.setJoystick(0, 0)}
          onButtonChange={(b, p) => input.current?.setMobileButton(b, p)}
        />
      )}

      <div
        style={{
          position: "fixed",
          top: 0,
          left: 0,
          width: "100%",
          height: "100%",
          zIndex: 998, // below controls
        }}
        onPointerDown={(e) => handleTap(e, true)}
        onPointerUp={(e) => handleTap(e, false)}
        onPointerLeave={(e) => handleTap(e, false)}
      />

      {/* Score overlay on the field */}
      <div
        style={{
          position: "fixed",
          top: "20px",
          left: "50%",
          transform: "translateX(-50%)",
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          gap: "30px",
          padding: "10px 30px",
          borderRadius: "12px",
          color: "white",
          fontSize: "42px",
          fontWeight: "bold",
          backgroundColor: "rgba(0,0,0,0.4)",
          boxShadow: "0 0 20px rgba(0,0,0,0.3)",
          zIndex: 997,
        }}
      >
        <span style={{ color: "blue" }}>{data ? data.score2 : score.blue}</span>
        {"  -  "}
        <span style={{ color: "red" }}>{data ? data.score1 : score.red}</span>
      </div>

      {/* Render the 3D pitch component full screen */}
      {data && <GameRenderer3D ref={rendererRef} data={data} />}
    </div>
  );
}