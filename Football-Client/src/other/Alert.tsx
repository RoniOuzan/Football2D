import type { AlertData } from "../client/jsons/alertTypes";

const ALERT_SIZES = {
  small: {
    padding: "14px 20px",
    title: "22px",
    sub: "14px",
    minWidth: "220px",
  },
  medium: {
    padding: "20px 28px",
    title: "32px",
    sub: "18px",
    minWidth: "320px",
  },
  large: {
    padding: "24px 32px",
    title: "44px",
    sub: "22px",
    minWidth: "420px",
  },
} as const;

interface Props {
    alertMessage: AlertData
}

function Alert({ alertMessage }: Props) {
  const size = ALERT_SIZES[alertMessage.size ?? "medium"];

  return (
    <div
      style={{
        position: "absolute",
        top: "50%",
        left: "50%",
        transform: "translate(-50%, -50%) scale(1)",
        background: "linear-gradient(180deg, rgba(20,20,20,0.7), rgba(0,0,0,0.5))",
        color: alertMessage.color,
        padding: size.padding,
        borderRadius: "20px",
        zIndex: 9999,
        minWidth: size.minWidth,
        maxWidth: "80vw",
        textAlign: "center",
        boxShadow: `
          0 0 40px ${alertMessage.color}40,
          inset 0 0 0 1px rgba(255,255,255,0.08)
        `,
        animation: "alert-pop 0.35s cubic-bezier(.2,.8,.2,1)",
      }}
    >
      <div
        style={{
          fontSize: size.title,
          fontWeight: 900,
          letterSpacing: "1.5px",
          marginBottom: alertMessage.subTitle ? "10px" : 0,
          textShadow: `0 0 12px ${alertMessage.color}80`,
        }}
      >
        {alertMessage.title}
      </div>

      {alertMessage.subTitle && (
        <div
          style={{
            fontSize: size.sub,
            opacity: 0.8,
            lineHeight: 1.4,
          }}
        >
          {alertMessage.subTitle}
        </div>
      )}

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

export default Alert;