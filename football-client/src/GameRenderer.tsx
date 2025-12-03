import React, { useRef, useEffect, useState } from "react";
import { JsonData, Team } from "./types";
import { pitchWidthUnits, pitchHeightUnits } from "./types";

interface GameRendererProps {
	width: number;
	height: number;
	data: JsonData;
}

const GameRenderer3D: React.FC<GameRendererProps> = ({ width, height, data }) => {
	const canvasRef = useRef<HTMLCanvasElement>(null);
	const [camera, setCamera] = useState({ x: 0, y: 0 });

	useEffect(() => {
		const canvas = canvasRef.current;
		if (!canvas) return;

		canvas.width = width;
		canvas.height = height;

		const ctx = canvas.getContext("2d");
		if (!ctx) return;

		const playerRadius = 0.75 * (canvas.height / 64);
		const goalDepth = (3 / pitchWidthUnits) * canvas.width;

		// ----------------------------
		// Smooth camera follow
		// ----------------------------
    // Inside useEffect, before drawing anything
    const effBallX = Math.max(Math.min(data.ball.position.x, 30), -30) + 50;
    const ballX_px = (effBallX / pitchWidthUnits) * (canvas.width - (2 * goalDepth));
    
    const effBallY = Math.max(Math.min(data.ball.position.y, 16), -16) + 32;
    const ballY_px = (effBallY / pitchHeightUnits) * canvas.height;

    // Camera X offset: how much to shift everything so the ball is centered
    setCamera(() => ({
      x: ballX_px - canvas.width / 2,
      y: ballY_px - canvas.height / 2,
    }));

		ctx.clearRect(0, 0, canvas.width, canvas.height);

		// --------------------------
		// Draw pitch (trapezoid perspective)
		// --------------------------
		const topLeft = perspectivePoint(goalDepth, 0, canvas, camera);
		const topRight = perspectivePoint(canvas.width - goalDepth, 0, canvas, camera);
		const bottomLeft = perspectivePoint(goalDepth, canvas.height, canvas, camera);
		const bottomRight = perspectivePoint(canvas.width - goalDepth, canvas.height, canvas, camera);

		ctx.fillStyle = "#006400";
		ctx.strokeStyle = "white";
		ctx.lineWidth = 2;

		ctx.beginPath();
		ctx.moveTo(topLeft.x, topLeft.y);
		ctx.lineTo(topRight.x, topRight.y);
		ctx.lineTo(bottomRight.x, bottomRight.y);
		ctx.lineTo(bottomLeft.x, bottomLeft.y);
		ctx.closePath();
		ctx.fill();
		ctx.stroke();

		// Halfway line
		const halfTop = perspectivePoint(canvas.width / 2, 0, canvas, camera);
		const halfBottom = perspectivePoint(canvas.width / 2, canvas.height, canvas, camera);
		ctx.beginPath();
		ctx.moveTo(halfTop.x, halfTop.y);
		ctx.lineTo(halfBottom.x, halfBottom.y);
		ctx.stroke();

		// Center circle
		drawPerspectiveCircle(ctx, canvas, canvas.width / 2, canvas.height / 2, (10 / pitchHeightUnits) * canvas.height, camera);

		// Penalty boxes
		const penaltyWidth = (16.5 / pitchWidthUnits) * canvas.width;
		const penaltyHeight = (40.3 / pitchHeightUnits) * canvas.height;
		const penaltyYOffset = (canvas.height - penaltyHeight) / 2;

		drawTrapezoidRect(ctx, canvas, goalDepth, penaltyYOffset, penaltyWidth, penaltyHeight, camera);
		drawTrapezoidRect(ctx, canvas, canvas.width - goalDepth - penaltyWidth, penaltyYOffset, penaltyWidth, penaltyHeight, camera);

		// 6-yard boxes
		const sixYardWidth = (5.5 / pitchWidthUnits) * canvas.width;
		const sixYardHeight = (18.3 / pitchHeightUnits) * canvas.height;
		const sixYardYOffset = (canvas.height - sixYardHeight) / 2;

		drawTrapezoidRect(ctx, canvas, goalDepth, sixYardYOffset, sixYardWidth, sixYardHeight, camera);
		drawTrapezoidRect(ctx, canvas, canvas.width - goalDepth - sixYardWidth, sixYardYOffset, sixYardWidth, sixYardHeight, camera);

		// Penalty spots
		const penaltySpotOffset = (11 / pitchWidthUnits) * canvas.width;
		const leftPenalty = perspectivePoint(goalDepth + penaltySpotOffset, canvas.height / 2, canvas, camera);
		const rightPenalty = perspectivePoint(canvas.width - goalDepth - penaltySpotOffset, canvas.height / 2, canvas, camera);

		ctx.fillStyle = "white";
		ctx.beginPath();
		ctx.arc(leftPenalty.x, leftPenalty.y, 3, 0, Math.PI * 2);
		ctx.fill();
		ctx.beginPath();
		ctx.arc(rightPenalty.x, rightPenalty.y, 3, 0, Math.PI * 2);
		ctx.fill();

		// Goals
		drawGoals(ctx, canvas, goalDepth, camera);

		// Ball
		const ball = convert(canvas, data.ball.position, goalDepth, camera);
		ctx.fillStyle = "white";
		ctx.beginPath();
		ctx.arc(ball.x, ball.y, 5, 0, Math.PI * 2);
		ctx.fill();

		// Players
		drawPlayers(canvas, ctx, data.team1, goalDepth, playerRadius, "red", camera);
		drawPlayers(canvas, ctx, data.team2, goalDepth, playerRadius, "blue", camera);

		// Heatmaps
		drawScores(canvas, ctx, data.team1, goalDepth, camera);

		// Defense line
		const defenseX_units = data.team1.teamStrategy.defenseLine;
		const pxLeft = goalDepth;
		const pxRight = canvas.width - goalDepth;
		const defenseX = pxLeft + ((defenseX_units + 50) / pitchWidthUnits) * (pxRight - pxLeft);
		const defenseLineTop = perspectivePoint(defenseX, 0, canvas, camera);
		const defenseLineBottom = perspectivePoint(defenseX, canvas.height, canvas, camera);

		ctx.strokeStyle = "yellow";
		ctx.lineWidth = 2;
		ctx.beginPath();
		ctx.moveTo(defenseLineTop.x, defenseLineTop.y);
		ctx.lineTo(defenseLineBottom.x, defenseLineBottom.y);
		ctx.stroke();
	}, [data, width, height, camera]);

	return <canvas ref={canvasRef} width={width} height={height} />;
};

// --------------------
// Helpers
// --------------------
function perspectivePoint(
	x: number,
	y: number,
	canvas: HTMLCanvasElement,
	camera: { x: number; y: number }
) {
	const topScale = 0.6;
	const bottomScale = 1.0;
	const t = y / canvas.height;
	const scale = topScale + (bottomScale - topScale) * t;

	const centerX = canvas.width / 2;
	const newX = centerX + (x - centerX - camera.x) * scale;
	const newY = y + camera.y;
	return { x: newX, y: newY };
}

function drawTrapezoidRect(
	ctx: CanvasRenderingContext2D,
	canvas: HTMLCanvasElement,
	left: number,
	top: number,
	width: number,
	height: number,
	camera: { x: number; y: number }
) {
	const topLeft = perspectivePoint(left, top, canvas, camera);
	const topRight = perspectivePoint(left + width, top, canvas, camera);
	const bottomLeft = perspectivePoint(left, top + height, canvas, camera);
	const bottomRight = perspectivePoint(left + width, top + height, canvas, camera);

	ctx.beginPath();
	ctx.moveTo(topLeft.x, topLeft.y);
	ctx.lineTo(topRight.x, topRight.y);
	ctx.lineTo(bottomRight.x, bottomRight.y);
	ctx.lineTo(bottomLeft.x, bottomLeft.y);
	ctx.closePath();
	ctx.stroke();
}

function drawPerspectiveCircle(
	ctx: CanvasRenderingContext2D,
	canvas: HTMLCanvasElement,
	cx: number,
	cy: number,
	r: number,
	camera: { x: number; y: number }
) {
	const steps = 40;
	ctx.beginPath();
	for (let i = 0; i <= steps; i++) {
		const angle = (i / steps) * Math.PI * 2;
		const px = cx + Math.cos(angle) * r;
		const py = cy + Math.sin(angle) * r;
		const { x, y } = perspectivePoint(px, py, canvas, camera);
		if (i === 0) ctx.moveTo(x, y);
		else ctx.lineTo(x, y);
	}
	ctx.stroke();
}

// Players & Ball
function convert(
	canvas: HTMLCanvasElement,
	position: { x: number; y: number },
	goalDepth: number,
	camera: { x: number; y: number }
) {
	const pitchLeft = goalDepth;
	const pitchRight = canvas.width - goalDepth;
	const x = pitchLeft + ((position.x + 50) / pitchWidthUnits) * (pitchRight - pitchLeft);
	const y = ((32 - position.y) / pitchHeightUnits) * canvas.height;
	return perspectivePoint(x, y, canvas, camera);
}

function drawPlayers(
	canvas: HTMLCanvasElement,
	ctx: CanvasRenderingContext2D,
	team: Team | undefined,
	goalDepth: number,
	radius: number,
	color: string,
	camera: { x: number; y: number }
) {
	if (!team) return;
	team.players.forEach((p, i) => {
		const pos = convert(canvas, p.position, goalDepth, camera);
		ctx.fillStyle = color;
		ctx.beginPath();
		ctx.arc(pos.x, pos.y, radius, 0, Math.PI * 2);
		ctx.fill();

		if (i === team.teamStrategy.chosenPlayerIndex) {
			ctx.strokeStyle = "yellow";
			ctx.lineWidth = 2;
			ctx.beginPath();
			ctx.arc(pos.x, pos.y, radius, 0, Math.PI * 2);
			ctx.stroke();
		}
	});
}

// Heatmap
function scoreToColor(value: number, min: number, max: number): string {
	if (max === min) return "white";
	const t = (value - min) / (max - min);
	const r = Math.floor(255 * t);
	const g = Math.floor(255 * (1 - Math.abs(t - 0.5) * 2));
	const b = Math.floor(255 * (1 - t));
	return `rgb(${r}, ${g}, ${b})`;
}

function drawScores(
	canvas: HTMLCanvasElement,
	ctx: CanvasRenderingContext2D,
	team: Team | undefined,
	goalDepth: number,
	camera: { x: number; y: number }
) {
	if (!team) return;
	const values = team.teamStrategy.scores.map(s => s.value);
	const max = Math.min(Math.max(...values), 50);

	team.teamStrategy.scores.forEach(entry => {
		const pos = convert(canvas, entry.key, goalDepth, camera);
		const color = scoreToColor(entry.value, -120, max);
		ctx.fillStyle = color;
		ctx.globalAlpha = 0.3;
		const sizeX = (canvas.width - 2 * goalDepth) / 40;
		const sizeY = canvas.height / 40;
		ctx.fillRect(pos.x - sizeX / 2, pos.y - sizeY / 2, sizeX, sizeY);
		ctx.globalAlpha = 1.0;
	});
}

// Goals
function drawGoals(ctx: CanvasRenderingContext2D, canvas: HTMLCanvasElement, goalDepth: number, camera: { x: number; y: number }) {
	const goalWidth = (7.3 / pitchHeightUnits) * canvas.height;

	// LEFT GOAL
	ctx.strokeStyle = "red";
	ctx.lineWidth = 4;
	let topLeft = perspectivePoint(0, (canvas.height - goalWidth) / 2, canvas, camera);
	let topRight = perspectivePoint(goalDepth, (canvas.height - goalWidth) / 2, canvas, camera);
	let bottomLeft = perspectivePoint(0, (canvas.height + goalWidth) / 2, canvas, camera);
	let bottomRight = perspectivePoint(goalDepth, (canvas.height + goalWidth) / 2, canvas, camera);

	ctx.beginPath();
	ctx.moveTo(topLeft.x, topLeft.y);
	ctx.lineTo(bottomLeft.x, bottomLeft.y);
	ctx.lineTo(bottomRight.x, bottomRight.y);
	ctx.lineTo(topRight.x, topRight.y);
	ctx.closePath();
	ctx.stroke();

	// RIGHT GOAL
	ctx.strokeStyle = "blue";
	topLeft = perspectivePoint(canvas.width, (canvas.height - goalWidth) / 2, canvas, camera);
	topRight = perspectivePoint(canvas.width - goalDepth, (canvas.height - goalWidth) / 2, canvas, camera);
	bottomLeft = perspectivePoint(canvas.width, (canvas.height + goalWidth) / 2, canvas, camera);
	bottomRight = perspectivePoint(canvas.width - goalDepth, (canvas.height + goalWidth) / 2, canvas, camera);

	ctx.beginPath();
	ctx.moveTo(topLeft.x, topLeft.y);
	ctx.lineTo(bottomLeft.x, bottomLeft.y);
	ctx.lineTo(bottomRight.x, bottomRight.y);
	ctx.lineTo(topRight.x, topRight.y);
	ctx.closePath();
	ctx.stroke();
}

export default GameRenderer3D;
