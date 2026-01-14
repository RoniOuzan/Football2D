import { useRef, useState, useEffect } from "react";
import { FPS } from "./Game";

export default function useReplayController() {
  const [frames, setFrames] = useState<any[]>([]);
  const [active, setActive] = useState(false);
  const [currentIndex, setCurrentIndex] = useState(0);
  const intervalRef = useRef<number | null>(null);

  // Automatically stop playback if frames change and index is out of range
  useEffect(() => {
    if (currentIndex >= frames.length) {
      stop();
      setCurrentIndex(frames.length - 1);
    }
  }, [frames]);

  // Start playback
  const start = (frames: any[]) => {
    if (!frames.length) return;
    stop(); // ensure no duplicate intervals
    setFrames(frames);
    setActive(true);
    setCurrentIndex(0);

    intervalRef.current = window.setInterval(() => {
      setCurrentIndex((i) => {
        if (i + 1 >= frames.length) {
          return 0;
        }
        return i + 1;
      });
    }, 1000 / FPS);
  };

  // Stop playback
  const stop = () => {
    setActive(false);
    if (intervalRef.current !== null) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  };

  // Get current frame
  const getCurrentFrame = () => frames[currentIndex] || null;

  return {
    frames,
    active,
    currentIndex,
    start,
    stop,
    getCurrentFrame,
  };
}
