import { useRef } from "react";
import { motion, AnimatePresence } from "framer-motion";
import Client from "../client/Client";
import GameRenderer3D, {
  type GameRenderer3DHandle,
} from "../renderer/GameRenderer";
import MobileControls from "./MobileControls";
import { isMobile } from "../App";
import { screenToWorld } from "../renderer/CameraUtil";
import { getClientsTeam, type GameData } from "../client/jsons/gameTypes";
import Scorebar from "./Scorebar";

export const FPS = 30;

interface GameProps {
  client: Client;
  data: GameData;
}

export default function FixedJoystick({ client, data }: GameProps) {
  const rendererRef = useRef<GameRenderer3DHandle>(null);

  function handleTap(e: React.MouseEvent, pressed: boolean) {
    if (!data || !rendererRef.current || !client.getInput()) return;

    const canvas = rendererRef.current.getCanvas();
    if (!canvas) return;

    if (!pressed) {
      client.getInput().setClick(null);
      return;
    }
    const team = getClientsTeam(data);

    if (!team) return;

    const camera = team.teamStrategy.cameraManager.position;
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
        overscrollBehavior: "none",
      }}
    >
      {isMobile && data.client > 0 && (
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

      <Scorebar data={data} />

      <AnimatePresence>
        {data.spectators.length > 0 && (
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: 20 }}
            style={{
              position: "fixed",
              top: "12px",
              right: "12px",
              display: "flex",
              alignItems: "center",
              gap: "8px",
              padding: "6px 12px",
              borderRadius: "20px",
              backgroundColor: "rgba(0,0,0,0.4)",
              backdropFilter: "blur(4px)",
              color: "white",
              fontSize: "18px",
              fontWeight: "500",
              zIndex: 996,
              border: "1px solid rgba(255,255,255,0.1)"
            }}
          >
            <span style={{ fontSize: "14px", opacity: 0.8 }}>👁️</span>
            <span style={{ fontSize: "18px" }}>{data.spectators.length}</span>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Render the 3D pitch component full screen */}
      {data && <GameRenderer3D ref={rendererRef} data={data} />}
    </div>
  );
}
