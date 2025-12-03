import { useRef, useEffect, useState } from "react";
import Client from "./Client";
import InputController from "./InputController";
import { JsonData } from "./types";
import GameRenderer3D from "./GameRenderer";

export const FPS = 30;

export default function Game() {
	const [score, ] = useState({ blue: 0, red: 0 });
	const [data, setData] = useState<JsonData | null>(null);

	const client = useRef<Client | null>(null);
	const input = useRef<InputController | null>(null);

	useEffect(() => {
		if (client.current) return;

		client.current = new Client((gameState) => {
			setData(gameState);
		});

		client.current.connect();
		input.current = new InputController(client.current);
	}, []);

	return (
		<div style={{ width: "100vw", height: "100vh", position: "relative" }}>
			{/* Score overlay on the field */}
			<div
				style={{
					position: "absolute",
					top: "20px",
					left: "50%",
					transform: "translateX(-50%)",
					display: "flex",
					justifyContent: "center",
					alignItems: "center",
					gap: "30px",
					padding: "10px 30px",
					borderRadius: "12px",
					color: "white",
					fontSize: "42px",
					fontWeight: "bold",
					backgroundColor: "rgba(0,0,0,0.4)",
					boxShadow: "0 0 20px rgba(0,0,0,0.3)",
				}}
			>
				<span style={{ color: "blue" }}>{data ? data.score2 : score.blue}</span>
				{"  -  "}
				<span style={{ color: "red" }}>{data ? data.score1 : score.red}</span>
			</div>

			{/* Render the 3D pitch component full screen */}
			{data && <GameRenderer3D data={data} />}
		</div>
	);
}
