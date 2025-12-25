import { useEffect } from "react";
import Game from "./game/Game";

export let isMobile = false;

function App() {
  
  useEffect(() => {
    const preventScroll = (e: TouchEvent) => e.preventDefault();
    document.addEventListener("touchmove", preventScroll, { passive: false });

    return () => {
      document.removeEventListener("touchmove", preventScroll);
    };
  }, []);

  useEffect(() => {
    const ua = navigator.userAgent;
    isMobile = /Mobi|Android|iPhone|iPad|iPod|Opera Mini|IEMobile/i.test(ua);
  }, [navigator.userAgent]);

  return (
    <div className="app-root">
      {/* <h1>⚽ Football 2D</h1> */}
      <Game />
    </div>
  );
}

export default App;