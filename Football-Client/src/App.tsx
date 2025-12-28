import { useEffect, useRef, useState } from "react";
import Game from "./game/Game";
import Client from "./client/Client";
import type { ServerMessage } from "./client/wsMessages";
import Lobby from "./lobby/Lobby";

export let isMobile = false;
export let isPortrait = window.innerHeight > window.innerWidth;

function App() {
  const [viewport, setViewport] = useState({
    width: window.innerWidth,
    height: window.innerHeight,
  });

  const [msg, setMsg] = useState<ServerMessage | null>(null);
  const client = useRef<Client | null>(null);

  useEffect(() => {
    if (client.current) return;

    client.current = new Client(setMsg);
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

  if (!client.current || !msg) {
    return <LoadingScreen />;
  }

  let content: React.ReactNode;

  switch (msg.type) {
    case "lobby":
      content = (
        <Lobby client={client.current} data={msg.data} />
      );
      break;

    case "game":
      content = <Game client={client.current} data={msg.data} />;
      break;

    case "error":
      content = <ErrorScreen message={msg.data.message} />;
      break;

    default:
      content = null;
  }

  console.log(msg.type);
  
  return (
    <div
      style={{
        width: viewport.width,
        height: viewport.height,
        overflow: "hidden",
        background: "black",
        position: "relative",
      }}
    >
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

function ErrorScreen({ message }: { message: string }) {
  return (
    <div
      style={{
        width: "100vw",
        height: "100vh",
        background: "black",
        color: "red",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
      }}
    >
      {message}
    </div>
  );
}
