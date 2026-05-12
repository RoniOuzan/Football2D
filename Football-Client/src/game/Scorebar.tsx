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
          backgroundColor: "#DC2626",
          borderRadius: "12px 0px 0px 12px",
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

      {/* TIME POPDOWN */}
      <AnimatePresence>
        {addedTime && (
          <motion.div
            initial={{ opacity: 0.3, y: "-100%", x: "-50%" }}
            animate={{ opacity: 1, y: 0, x: "-50%" }}
            exit={{ opacity: 0.3, y: "-100%", x: "-50%" }}
            transition={{ duration: 0.2 }}
            style={{
              position: "absolute",
              top: "100%",
              left: "50%",
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
                minWidth: "80px",
                background: "linear-gradient(180deg, #a9a9a9 0%, #ffffff 50%)",
                borderRadius: "0 0 0 10px",
              }}
            >
              {formatTime(addedTime)}
            </span>
            {/* +ADDED */}
            <span
              style={{
                color: "white",
                padding: "4px 8px",
                textAlign: "center",
                minWidth: "38px",
                background: "linear-gradient(180deg, #111 0%, #313131 70%)",
                borderRadius: "0 0 10px 0",
              }}
            >
              +{Math.round(data.addedTime / 60)}
            </span>
          </motion.div>
        )}
      </AnimatePresence>

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
      </div>

      {/* RED TEAM */}
      <div
        style={{
          minWidth: "64px",
          padding: "0 14px",
          backgroundColor: "#1D4ED8",
          borderRadius: "0px 12px 12px 0px",
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
    </div>
  );
}