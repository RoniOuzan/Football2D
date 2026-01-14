import { useEffect, useRef, useState } from "react";
import Client from "./client/Client";
import type { AlertData } from "./client/jsons/alertTypes";
import type { CountdownData } from "./client/jsons/countdownTypes";
import type { ServerMessage } from "./client/wsMessages";
import Game from "./game/Game";
import Lobby from "./lobby/Lobby";
import Alert from "./other/Alert";
import Countdown from "./other/Countdown";
import type { GameData } from "./client/jsons/gameTypes";
import type { LobbyData } from "./client/jsons/lobbyTypes";

export let isMobile = false;
export let isPortrait = window.innerHeight > window.innerWidth;

function App() {
  const [viewport, setViewport] = useState({
    width: window.innerWidth,
    height: window.innerHeight,
  });

  const [lobbyData, setLobbyData] = useState<LobbyData | null>(null);
  const [gameData, setGameData] = useState<GameData | null>(null);
  const [alertMessage, setAlertMessage] = useState<AlertData | null>(null);
  const [countdown, setCountdown] = useState<CountdownData | null>(null);

  const client = useRef<Client | null>(null);

  useEffect(() => {
    if (client.current) return;

    client.current = new Client(handleServerMessage);
  }, []);

  useEffect(() => {
    const update = () => {
      const w = window.visualViewport?.width ?? window.innerWidth;
      const h = window.visualViewport?.height ?? window.innerHeight;
      setViewport({ width: w, height: h });
      isPortrait = h > w;
    };

    update();
    window.visualViewport?.addEventListener("resize", update);
    window.addEventListener("resize", update);
    window.addEventListener("orientationchange", update);

    return () => {
      window.visualViewport?.removeEventListener("resize", update);
      window.removeEventListener("resize", update);
      window.removeEventListener("orientationchange", update);
    };
  }, []);

  useEffect(() => {
    isMobile = /Mobi|Android|iPhone|iPad|iPod/i.test(navigator.userAgent);
  }, []);

  useEffect(() => {
    const loader = document.getElementById("initial-loader");
    if (!loader) return;

    loader.classList.add("fade");
    setTimeout(() => loader.remove(), 300);
  }, []);

  const handleServerMessage = (msg: ServerMessage) => {
    switch (msg.type) {
      case "lobby":
        setLobbyData(msg.data);
        break;

      case "game":
        setGameData(msg.data);
        break;

      case "replay":
        setGameData(prev => prev ? {
          ...prev,
          replay: msg.data
        } : null);
        break;

      case "alert":
        setAlertMessage(msg.data);
        setTimeout(() => setAlertMessage(null), msg.data.time * 1000);
        break;

      case "countdown":
        setCountdown(msg.data);
        break;
    }
  };

  if (!client.current || (!lobbyData && !gameData)) {
    return <LoadingScreen />;
  }

  let content = null;

  if (gameData) {
    content = <Game client={client.current} game={gameData} />;
  } else if (lobbyData) {
    content = <Lobby client={client.current} data={lobbyData} />;
  }

  return (
    <div
      style={{
        width: viewport.width,
        height: viewport.height,
        background: "black",
        position: "relative",
      }}
    >
      {alertMessage && <Alert alertMessage={alertMessage} />}
      {countdown && <Countdown data={countdown} onFinish={() => setCountdown(null)} />}

      <div
        style={{
          position: "absolute",
          width: isPortrait ? viewport.height : viewport.width,
          height: isPortrait ? viewport.width : viewport.height,
          top: "50%",
          left: "50%",
          transform: isPortrait
            ? "translate(-50%, -50%) rotate(90deg)"
            : "translate(-50%, -50%)",
          transformOrigin: "center",
        }}
      >
        {content}
      </div>
    </div>
  );
}

export default App;

function LoadingScreen() {
  return (
    <div
      style={{
        position: "fixed",
        inset: 0,
        background: "radial-gradient(circle at center, #1b1b1b, #000)",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        color: "white",
        fontFamily: "system-ui, sans-serif",
        zIndex: 9999,
      }}
    >
      <div
        style={{
          fontSize: "42px",
          fontWeight: 800,
          marginBottom: "16px",
          letterSpacing: "1px",
        }}
      >
        ⚠ No Connection
      </div>

      <div
        style={{
          fontSize: "18px",
          opacity: 0.75,
          marginBottom: "30px",
        }}
      >
        Trying to reconnect to the server…
      </div>

      {/* Spinner */}
      <div
        style={{
          width: "48px",
          height: "48px",
          borderRadius: "50%",
          border: "4px solid rgba(255,255,255,0.2)",
          borderTopColor: "#fff",
          animation: "spin 0.9s ease infinite",
        }}
      />

      <style>
        {`
          @keyframes spin {
            to { transform: rotate(360deg); }
          }
        `}
      </style>
    </div>
  );
}
