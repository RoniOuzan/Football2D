import { useRef, useState } from "react";
import Client from "../client/Client";
import GameRenderer3D, {
  type GameRenderer3DHandle,
} from "../renderer/GameRenderer";
import MobileControls from "./MobileControls";
import { isMobile } from "../App";
import { screenToWorld } from "../renderer/CameraUtil";
import { getClientsTeam, type GameData } from "../client/jsons/gameTypes";

export const FPS = 30;

interface GameProps {
  client: Client;
  data: GameData;
}

export default function FixedJoystick({ client, data }: GameProps) {
  const [score] = useState({ blue: 0, red: 0 });
  const rendererRef = useRef<GameRenderer3DHandle>(null);

  function handleTap(e: React.MouseEvent, pressed: boolean) {
    if (!data || !rendererRef.current || !client.getInput()) return;

    const canvas = rendererRef.current.getCanvas();
    if (!canvas) return;

    if (!pressed) {
      client.getInput().setClick(null);
      return;
    }
    const camera = getClientsTeam(data).teamStrategy.cameraManager.position;
    const world = screenToWorld(canvas, { x: e.clientX, y: e.clientY }, camera);
    client.getInput().setClick(world);
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
          onMove={(x, y) => client.getInput().setJoystick(x, y)}
          onStop={() => client.getInput().setJoystick(0, 0)}
          onButtonChange={(b, p) => client.getInput().setMobileButton(b, p)}
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
