import { Joystick } from "react-joystick-component";
import { FPS } from "./Game";

interface MobileControlsProps {
  onMove: (x: number, y: number) => void;
  onStop: () => void;
  onButtonChange: (button: string, pressed: boolean) => void;
}

export default function MobileControls({ onMove, onStop, onButtonChange }: MobileControlsProps) {
  return (
    <>
      {/* Joystick */}
      <div
        style={{
          position: "fixed",
          bottom: 40,
          left: 40,
          zIndex: 999,
          width: 120,
          height: 120,
          userSelect: "none"
        }}
      >
        <Joystick
          size={120}
          stickSize={60}
          baseColor="rgba(20, 20, 20, 0.46)"
          stickColor="rgba(255, 255, 255, 1)"
          throttle={1000 / FPS}
          move={(stick) => {
            if (!stick.x || !stick.y) return;
            onMove(stick.x, stick.y);
          }}
          stop={() => onStop()}
        />
      </div>

      {/* Action buttons */}
      <div
        style={{
          position: "fixed",
          bottom: 20,
          right: 15,
          zIndex: 999,
          display: "grid",
          gridTemplateColumns: "repeat(3, 70px)",
          gridTemplateRows: "repeat(3, 70px)",
          gap: 0,
        }}
      >
        {["", "Shoot", "", "Pass", "", "Cross", "", "Through", ""].map((action, index) => (
          <button
            key={index}
            onPointerDown={() => action && onButtonChange(action.toLowerCase(), true)}
            onPointerUp={() => action && onButtonChange(action.toLowerCase(), false)}
            onPointerLeave={() => action && onButtonChange(action.toLowerCase(), false)}
            style={{
              width: 70,
              height: 70,
              borderRadius: "50%",
              backgroundColor: action ? "rgba(20, 20, 20, 0.46)" : "transparent",
              color: "white",
              fontWeight: "bold",
              fontSize: 14,
              display: "flex",
              justifyContent: "center",
              alignItems: "center",
              textAlign: "center",
              border: "none",
              boxShadow: action ? "0 3px 16px rgba(0,0,0,0.5)" : "none",
              pointerEvents: action ? "auto" : "none",
              touchAction: "none",
              transition: "transform 0.1s, background-color 0.1s",
              userSelect: "none",
            }}
            onPointerDownCapture={(e) => {
              e.currentTarget.style.transform = "scale(0.9)";
              e.currentTarget.style.backgroundColor = "rgba(20, 20, 20, 0.74)";
            }}
            onPointerUpCapture={(e) => {
              e.currentTarget.style.transform = "scale(1)";
              e.currentTarget.style.backgroundColor = "rgba(20, 20, 20, 0.46)";
            }}
          >
            {action}
          </button>
        ))}
      </div>
    </>
  );
}
