import { useState } from "react";

interface JoystickProps {
  onMove: (x: number, y: number) => void;
  onStop: () => void;
}

export default function Joystick({ onMove, onStop }: JoystickProps) {
  const baseX = 100; // fixed base position X
  const baseY = window.innerHeight - 100; // fixed base position Y (bottom-left)
  const maxRadius = 60; // max stick distance and size of the whole joystick

  const [stickPos, setStickPos] = useState({ x: 0, y: 0 });
  const [dragging, setDragging] = useState(false);

  const handleStart = (e: React.TouchEvent | React.MouseEvent) => {
    setDragging(true);
    handleMove(e);
  };

  const handleMove = (e: React.TouchEvent | Touch | React.MouseEvent) => {
    if (!dragging) return;
    const clientX = "touches" in e ? e.touches[0].clientX : (e as React.MouseEvent).clientX;
    const clientY = "touches" in e ? e.touches[0].clientY : (e as React.MouseEvent).clientY;

    let dx = clientX - baseX;
    let dy = clientY - baseY;

    // limit radius
    const dist = Math.sqrt(dx * dx + dy * dy);
    const ratio = dist > maxRadius ? maxRadius / dist : 1;
    dx *= ratio;
    dy *= ratio;

    setStickPos({ x: dx, y: dy });
    onMove(dx / maxRadius, dy / maxRadius); // normalized -1 to 1
  };

  const handleEnd = () => {
    setDragging(false);
    setStickPos({ x: 0, y: 0 });
    onStop();
  };

  return (
    <div
      style={{
        position: "fixed",
        left: baseX - maxRadius,
        top: baseY - maxRadius,
        width: maxRadius * 2,
        height: maxRadius * 2,
        borderRadius: "50%",
        backgroundColor: "rgba(20,20,20,0.3)",
        touchAction: "none",
        userSelect: "none",
        zIndex: 999,
      }}
      onMouseDown={handleStart}
      onTouchStart={handleStart}
      onMouseMove={handleMove}
      onTouchMove={handleMove}
      onMouseUp={handleEnd}
      onTouchEnd={handleEnd}
      onMouseLeave={handleEnd}
    >
      {/* Stick */}
      <div
        style={{
          position: "absolute",
          left: maxRadius + stickPos.x - (maxRadius / 2),
          top: maxRadius + stickPos.y - (maxRadius / 2),
          width: maxRadius,
          height: maxRadius,
          borderRadius: "50%",
          backgroundColor: "rgba(255,255,255,0.9)",
        }}
      />
    </div>
  );
}
