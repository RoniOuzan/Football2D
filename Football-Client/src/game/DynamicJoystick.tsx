import { useState } from "react";

interface JoystickProps {
  onMove: (x: number, y: number) => void;
  onStop: () => void;
}

export default function DynamicJoystick({ onMove, onStop }: JoystickProps) {
  const maxRadius = 60;
  const stickSizePercent = 0.4;
  const stickSize = maxRadius * 2 * stickSizePercent;

  const [basePos, setBasePos] = useState<{ x: number; y: number } | null>(null);
  const [stickPos, setStickPos] = useState({ x: 0, y: 0 });
  const [dragging, setDragging] = useState(false);

  const handleStart = (e: React.TouchEvent | React.MouseEvent) => {
    let clientX = "touches" in e ? e.touches[0].clientX : (e as React.MouseEvent).clientX;
    let clientY = "touches" in e ? e.touches[0].clientY : (e as React.MouseEvent).clientY;
    
    setBasePos({ x: clientX, y: clientY });
    setDragging(true);
    setStickPos({ x: 0, y: 0 });
  };

  const handleMove = (e: React.TouchEvent | Touch | React.MouseEvent) => {
    if (!dragging || !basePos) return;

    let clientX = "touches" in e ? e.touches[0].clientX : (e as React.MouseEvent).clientX;
    let clientY = "touches" in e ? e.touches[0].clientY : (e as React.MouseEvent).clientY;

    let dx = clientX - basePos.x;
    let dy = clientY - basePos.y;

    const dist = Math.sqrt(dx * dx + dy * dy);
    const ratio = dist > maxRadius ? maxRadius / dist : 1;
    dx *= ratio;
    dy *= ratio;

    setStickPos({ x: dx, y: dy });
    onMove(dx / maxRadius, dy / maxRadius);
  };

  const handleEnd = () => {
    setDragging(false);
    setStickPos({ x: 0, y: 0 });
    setBasePos(null);
    onStop();
  };

  return (
    <div
      style={{
        position: "fixed",
        bottom: 0,
        left: 0,
        width: "40%",
        height: "100%",
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
      {/* Render joystick only when basePos exists */}{" "}
      {basePos && (
        <div
          style={{
            position: "absolute",
            left: basePos.x - maxRadius,
            top: basePos.y - maxRadius,
            width: maxRadius * 2,
            height: maxRadius * 2,
            borderRadius: "50%",
            backgroundColor: "rgba(20,20,20,0.3)",
          }}
        >
          <div
            style={{
              position: "absolute",
              left: maxRadius + stickPos.x - stickSize / 2,
              top: maxRadius + stickPos.y - stickSize / 2,
              width: stickSize,
              height: stickSize,
              borderRadius: "50%",
              backgroundColor: "rgba(255,255,255,0.9)",
            }}
          />
        </div>
      )}
    </div>
  );
}
