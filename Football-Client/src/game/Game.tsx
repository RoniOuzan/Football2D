import { AnimatePresence, motion } from "framer-motion";
import { useEffect, useRef } from "react";
import { isMobile } from "../App";
import Client from "../client/Client";
import { getClientsTeam, type GameData } from "../client/jsons/gameTypes";
import { screenToWorld } from "../renderer/CameraUtil";
import GameRenderer3D, { type GameRenderer3DHandle } from "../renderer/GameRenderer";
import MobileControls from "./MobileControls";
import Scorebar from "./Scorebar";
import useReplayController from "./useReplayController";

export const FPS = 30;

interface GameProps {
  client: Client;
  game: GameData;
}

export default function Game({ client, game }: GameProps) {
  const rendererRef = useRef<GameRenderer3DHandle>(null);

  const replayController = useReplayController();

  useEffect(() => {
    if (game.replay?.active && game.replay.frames.length > 0 && !replayController.active) {
      replayController.start(game.replay.frames);
    }
  }, [game.replay, replayController]);

  const currentFrame = replayController.active ? replayController.getCurrentFrame() : game;
  if (!currentFrame) return null;

  const handleTap = (e: React.MouseEvent, pressed: boolean) => {
    if (!currentFrame || !rendererRef.current || !client.getInput()) return;
    const canvas = rendererRef.current.getCanvas();
    if (!canvas) return;

    if (!pressed) {
      client.getInput().setClick(null);
      return;
    }

    const team = getClientsTeam(currentFrame);
    if (!team) return;

    const camera = team.teamStrategy.cameraManager.position;
    const world = screenToWorld(canvas, { x: e.clientX, y: e.clientY }, camera);
    client.getInput().setClick(world);
  };

  return (
    <div style={{ width: "100%", height: "100%", position: "fixed" }}>
      {isMobile && game?.client! > 0 && (
        <MobileControls
          onMove={(x, y) => client.getInput().setJoystick(x, y)}
          onStop={() => client.getInput().setJoystick(0, 0)}
          onButtonChange={(b, p) => client.getInput().setMobileButton(b, p)}
        />
      )}

      <div
        style={{ position: "fixed", top: 0, left: 0, width: "100%", height: "100%" }}
        onPointerDown={(e) => handleTap(e, true)}
        onPointerUp={(e) => handleTap(e, false)}
        onPointerLeave={(e) => handleTap(e, false)}
      />

      <AnimatePresence>
        {game.spectators.length > 0 && (
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
            <span style={{ fontSize: "18px" }}>{game.spectators.length}</span>
          </motion.div>
        )}
      </AnimatePresence>

      <Scorebar data={game} />
      <GameRenderer3D ref={rendererRef} data={currentFrame} />
    </div>
  );
}
