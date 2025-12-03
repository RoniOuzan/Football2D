import { useRef, useEffect, useState } from "react";
import Client from "./Client";
import InputController from "./InputController";
import { JsonData } from "./types";
import GameRenderer3D from "./GameRenderer";

export const FPS = 30;

export default function Game() {
	const [score, setScore] = useState({ blue: 0, red: 0 });
	const [data, setData] = useState<JsonData | null>(null);

	const client = useRef<Client | null>(null);
	const input = useRef<InputController | null>(null);

	useEffect(() => {
		if (client.current) return;

		client.current = new Client((gameState) => {
			setData(gameState);
			setScore({ blue: gameState.score2, red: gameState.score1 });
		});

		client.current.connect();
		input.current = new InputController(client.current);
	}, []);

	return (
		<div>
			<div
				style={{
					display: "flex",
					justifyContent: "center",
					alignItems: "center",
					gap: "30px",
					padding: "10px 30px",
					marginBottom: "15px",
					borderRadius: "12px",
					color: "white",
					fontSize: "42px",
					fontWeight: "bold",
					width: "fit-content",
					marginLeft: "auto",
					marginRight: "auto",
					boxShadow: "0 0 20px rgba(0,0,0,0.3)",
					transition: "background 0.2s, box-shadow 0.2s",
				}}
			>
				<span style={{ color: "blue" }}>{score.blue}</span>
				{"  -  "}
				<span style={{ color: "red" }}>{score.red}</span>
			</div>

			{/* Render the 3D pitch component */}
			{data && <GameRenderer3D width={1800} height={700} data={data} />}
		</div>
	);
}
