import { AnimatePresence, motion } from "framer-motion";
import type { GameData, Phase } from "../client/jsons/gameTypes";

function formatTime(seconds: number) {
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs.toString().padStart(2, "0")}`;
}

function getPhaseStopTime(phase: Phase) {
  switch (phase) {
    case "FIRST_HALF":
      return 45 * 60;
    case "SECOND_HALF":
      return 90 * 60;
    case "EXTRA_TIME":
      return 120 * 60;
    case "FINISH":
      return null;
  }
}

function getAddedTime(data: GameData) {
  const stopTime = getPhaseStopTime(data.phase);

  if (stopTime && data.matchTime >= stopTime) {
    return data.matchTime - stopTime;
  }
  return null;
}

interface Props {
  data: GameData;
}

export default function FixedJoystick({ data }: Props) {
  const addedTime = getAddedTime(data);

  return (
    <div
      style={{
        position: "fixed",
        top: "24px",
        left: "50%",
        transform: "translateX(-50%)",
        display: "flex",
        alignItems: "stretch",
        height: "52px",
        fontFamily: "Arial, Helvetica, sans-serif",
        zIndex: 997,
        pointerEvents: "none",
      }}
    >
      {/* BLUE TEAM */}
      <div
        style={{
          minWidth: "64px",
          padding: "0 14px",
          backgroundColor: "#1D4ED8",
          borderRadius: "12px 0px 0px 12px",
          color: "white",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontSize: "32px",
          fontWeight: 700,
        }}
      >
        {data.score2}
      </div>

      {/* CENTER BAR */}
      <div
        style={{
          minWidth: "116px",
          padding: "0 16px",
          background: "linear-gradient(180deg, #111 0%, #2d2d2d 70%)",
          color: "white",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          fontWeight: 700,
          position: "relative",
          borderLeft: "2px solid rgba(255, 255, 255, 0.25)",
          borderRight: "2px solid rgba(255, 255, 255, 0.25)",
        }}
      >
        {/* MATCH TIME */}
        <div
          style={{
            fontSize: "30px",
            lineHeight: "30px",
            letterSpacing: "0.5px",
          }}
        >
          {formatTime(data.matchTime)}
        </div>

        {/* ADDED TIME POPDOWN */}
        <AnimatePresence>
          {addedTime && (
            <motion.div
              initial={{ opacity: 0, y: -4 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -4 }}
              transition={{ duration: 0.18 }}
              style={{
                position: "absolute",
                top: "100%",
                borderTop: "1px solid #111",
                background: "linear-gradient(180deg, #d4d4d4 0%, #ffffff 70%)",
                borderRadius: "0 0 6px 6px",
                display: "flex",
                alignItems: "center",
                fontSize: "18px",
                fontWeight: 700,
                letterSpacing: "0.5px",
                boxShadow: "0 2px 4px rgba(0,0,0,0.35)",
              }}
            >
              {/* ELAPSED ADDED TIME */}
              <span
                style={{
                  color: "#111",
                  padding: "4px 8px",
                  textAlign: "center",
                }}
              >
                {formatTime(addedTime)}
              </span>
              {/* +ADDED */}
              <span
                style={{
                  background: "linear-gradient(180deg, #111 0%, #313131 70%)",
                  color: "white",
                  padding: "4px 8px",
                  minWidth: "36px",
                  textAlign: "center",
                  borderRadius: "0 0 6px 0",
                }}
              >
                +{Math.round(data.addedTime / 60)}
              </span>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* RED TEAM */}
      <div
        style={{
          minWidth: "64px",
          padding: "0 14px",
          backgroundColor: "#DC2626",
          borderRadius: "0px 12px 12px 0px",
          color: "white",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontSize: "32px",
          fontWeight: 700,
        }}
      >
        {data.score1}
      </div>
    </div>
  );
}