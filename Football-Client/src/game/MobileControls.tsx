import FixedJoystick from "./FixedJoystick";
import "./MobileControls.css";

interface MobileControlsProps {
  onMove: (x: number, y: number) => void;
  onStop: () => void;
  onButtonChange: (button: string, pressed: boolean) => void;
}

export default function MobileControls({
  onMove,
  onStop,
  onButtonChange,
}: MobileControlsProps) {
  // button sizes
  const bigSize = 100; // Sprint & Skill
  const smallSize = 70; // other actions

  // radius for surrounding buttons
  const radius = 125;

  // calculate positions around the central button
  const surrounding = [
    { action: "Shoot", angle: 90 }, // top-right
    { action: "Through", angle: 45 }, // top-left
    { action: "Pass", angle: 0 }, // bottom-left
  ].map(({ action, angle }) => {
    const rad = (angle * Math.PI) / 180;
    return {
      action,
      x: radius * Math.cos(rad),
      y: radius * Math.sin(rad),
    };
  });

  return (
    <>
      {/* Joystick */}
      <FixedJoystick onMove={onMove} onStop={onStop} />

      {/* Action buttons container */}
      <div
        style={{
          position: "fixed",
          bottom: 12,
          right: 12,
          zIndex: 999,
          width: 200,
          height: 200,
          pointerEvents: "none", // container itself should not block touches
        }}
      >
        {/* Big center button */}
        <ActionButton
          action="Sprint"
          onButtonChange={onButtonChange}
          size={bigSize}
          style={{
            position: "absolute",
            right: 0,
            bottom: 0,
            pointerEvents: "auto",
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
              position: "absolute",
              bottom: y,
              right: x,
              pointerEvents: "auto",
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

const ActionButton: React.FC<ActionButtonProps> = ({
  action,
  onButtonChange,
  size,
  style,
}) => {
  return (
    <button
      className="action-button"
      onPointerDown={() => action && onButtonChange(action.toLowerCase(), true)}
      onPointerUp={() => action && onButtonChange(action.toLowerCase(), false)}
      onPointerLeave={() =>
        action && onButtonChange(action.toLowerCase(), false)
      }
      style={{
        width: size,
        height: size,
        fontSize: size / 5,
        ...style,
      }}
    >
      {action}
    </button>
  );
};
