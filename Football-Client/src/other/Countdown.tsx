import { useState, useEffect } from "react";
import type { CountdownData } from "../client/jsons/countdownTypes";

interface Props {
    data: CountdownData;
    onFinish: () => void;
}

function Countdown({ data, onFinish }: Props) {
  const [current, setCurrent] = useState(data.from);

  useEffect(() => {
    const interval = setInterval(() => {
      setCurrent(prev => {
        if (prev <= data.to) {
          clearInterval(interval);
          onFinish();
          return data.to;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [data.from, data.to]);

  return (
    <div
      style={{
        position: "absolute",
        top: "50%",
        left: "50%",
        transform: "translate(-50%, -50%) scale(1)",
        background: "linear-gradient(180deg, rgba(20,20,20,0.7), rgba(0,0,0,0.5))",
        color: "yellow",
        padding: "24px 32px",
        borderRadius: "20px",
        zIndex: 100,
        minWidth: "320px",
        textAlign: "center",
        boxShadow: "0 0 40px yellow40, inset 0 0 0 1px rgba(255,255,255,0.08)",
        animation: "alert-pop 0.35s cubic-bezier(.2,.8,.2,1)",
      }}
    >
      <div
        style={{
          fontSize: "32px",
          fontWeight: 900,
          letterSpacing: "1.5px",
          textShadow: "0 0 12px yellow80",
        }}
      >
        {data.message.replace("%d", current.toString())}
      </div>

      <style>
        {`
          @keyframes alert-pop {
            from {
              opacity: 0;
              transform: translate(-50%, -50%) scale(0.9);
            }
            to {
              opacity: 1;
              transform: translate(-50%, -50%) scale(1);
            }
          }
        `}
      </style>
    </div>
  );
}

export default Countdown