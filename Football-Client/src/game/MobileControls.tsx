import Joystick from "./Joystick";

interface MobileControlsProps {
  onMove: (x: number, y: number) => void;
  onStop: () => void;
  onButtonChange: (button: string, pressed: boolean) => void;
}

export default function MobileControls({ onMove, onStop, onButtonChange }: MobileControlsProps) {
  // button sizes
  const bigSize = 100; // Sprint & Skill
  const smallSize = 70; // other actions

  // radius for surrounding buttons
  const radius = 125;

  // calculate positions around the central button
  const surrounding = [
    { action: "Shoot", angle: 90 },  // top-right
    { action: "Through", angle: 45 }, // top-left
    { action: "Pass", angle: 0 },   // bottom-left
  ].map(({ action, angle }) => {
    const rad = angle * Math.PI / 180;
    return {
      action,
      x: radius * Math.cos(rad),
      y: radius * Math.sin(rad),
    };
  });

  return (
    <>
      {/* Joystick */}
      <Joystick
        onMove={onMove}
        onStop={onStop}
      />

      {/* Action buttons container */}
      <div
        style={{
          position: "fixed",
          bottom: 12,
          right: 12,
          zIndex: 999,
          width: 200,
          height: 200,
        }}
      >
        {/* Big Sprint & Skill button in center */}
        <ActionButton
          action="Sprint"
          onButtonChange={onButtonChange}
          size={bigSize}
          style={{
            bottom: 0,
            right: 0,
          }}
        />

        {/* Surrounding buttons */}
        {surrounding.map(({ action, x, y }) => (
          <ActionButton
            key={action}
            action={action}
            onButtonChange={onButtonChange}
            size={smallSize}
            style={{
              bottom: y,
              right: x,
            }}
          />
        ))}
      </div>
    </>
  );
}

interface ActionButtonProps {
  action: string;
  onButtonChange: (button: string, pressed: boolean) => void;
  size: number;
  style?: React.CSSProperties;
}

const ActionButton: React.FC<ActionButtonProps> = ({ action, onButtonChange, size, style }) => {
  return (
    <button
      onPointerDown={() => action && onButtonChange(action.toLowerCase(), true)}
      onPointerUp={() => action && onButtonChange(action.toLowerCase(), false)}
      onPointerLeave={() => action && onButtonChange(action.toLowerCase(), false)}
      style={{
        width: size,
        height: size,
        position: "absolute",
        borderRadius: "50%",
        backgroundColor: "rgba(30,30,30,0.6)",
        border: "1px solid rgba(255, 255, 255, 0.22)",
        color: "white",
        fontWeight: "bold",
        fontSize: size / 5,
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        textAlign: "center",
        boxShadow: "0 4px 12px rgba(0,0,0,0.5)",
        pointerEvents: "auto",
        touchAction: "none",
        userSelect: "none",
        transition: "transform 0.1s, background-color 0.1s",
        ...style,
      }}
      onPointerDownCapture={(e) => {
        e.currentTarget.style.transform = "scale(0.9)";
        e.currentTarget.style.backgroundColor = "rgba(30,30,30,0.9)";
      }}
      onPointerUpCapture={(e) => {
        e.currentTarget.style.transform = "scale(1)";
        e.currentTarget.style.backgroundColor = "rgba(30,30,30,0.7)";
      }}
    >
      {action}
    </button>
  );
};
