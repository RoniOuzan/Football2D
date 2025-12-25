import { Joystick } from 'react-joystick-component';

interface MobileControlsProps {
  onMove: (x: number, y: number) => void;
  onStop: () => void;
  onButtonChange: (button: string, pressed: boolean) => void;
}

export default function MobileControls({ onMove, onStop, onButtonChange }: MobileControlsProps) {
  return (
    <div style={{ position: 'fixed', bottom: 50, left: 50, zIndex: 999 }}>
      <Joystick
        size={80}
        baseColor="gray"
        stickColor="yellow"
        move={stick => {
          if (!stick.x || !stick.y) return;

          onMove(stick.x, stick.y);
        }}
        stop={() => onStop()}
      />
      
      <div
        style={{
          position: "fixed",
          bottom: 20,
          right: 20,
          zIndex: 999,
          display: "grid",
          gridTemplateColumns: "repeat(3, 1fr)",
          gridTemplateRows: "repeat(3, 1fr)",
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
              width: 60,
              height: 60,
              borderRadius: "50%",
              backgroundColor: action ? "orange" : "transparent",
              color: "white",
              fontWeight: "bold",
              fontSize: 13,
              display: "flex",
              justifyContent: "center",
              alignItems: "center",
              textAlign: "center",
              border: "none",
              pointerEvents: action ? "auto" : "none", // empty cells not clickable
            }}
          >
            {action}
          </button>
        ))}
      </div>
    </div>
  );
}
