import { useEffect, useState } from "react";
import Game from "./game/Game";

export let isMobile = false;
export let isPortrait = window.innerHeight > window.innerWidth;

function App() {
  const [viewport, setViewport] = useState({ width: window.innerWidth, height: window.innerHeight });

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
        <Game />
      </div>
    </div>
  );
}

export default App;
