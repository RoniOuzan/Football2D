import React, { useRef, useEffect, useState } from "react";
import { JsonData, Team, Translation2d } from "./types";
import { pitchWidthUnits, pitchHeightUnits } from "./types";

const HEIGHT_SCALE = 0.45;     // flatten pitch (FIFA look)
const TOP_SCALE    = 0.25;     // must be > 0     // top is smaller than 0 (stronger perspective)
const BOTTOM_SCALE = 1.00;     // bottom is full width
const ZOOM         = 1.7;     // medium tele zoom
const TILT_Y       = 0;     // camera shift upward (bird's eye)

interface GameRendererProps {
	data: JsonData;
}

const GameRenderer3D: React.FC<GameRendererProps> = ({ data }) => {
	const canvasRef = useRef<HTMLCanvasElement>(null);
	const [camera, setCamera] = useState({ x: 0, y: 0 });

	useEffect(() => {
		const canvas = canvasRef.current;
		if (!canvas) return;

		canvas.width = window.innerWidth;
		canvas.height = window.innerHeight;

		const ctx = canvas.getContext("2d");
		if (!ctx) return;

		const goalDepth = (3 / pitchWidthUnits) * canvas.width;
    const fieldWidthPX = canvas.width - 2 * goalDepth;

		// ----------------------------
		// Smooth camera follow
		// ----------------------------
    // Inside useEffect, before drawing anything
    const effBallX = Math.max(Math.min(data.ball.position.x, 25), -25) * 0.5;
    const ballX_px = (effBallX / pitchWidthUnits / 2) * fieldWidthPX;
    
    const effBallY = Math.max(Math.min(data.ball.position.y, 16), -16) * 0.5;
    const ballY_px = (effBallY / pitchHeightUnits / 2) * canvas.height;

    setCamera(() => ({
      x: ballX_px,
      y: ballY_px,
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
		drawPerspectiveCircle(canvas, ctx, { x: 0, y: 0}, 9.15, goalDepth, camera);

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
    fillCircle(canvas, ctx, data.ball.position, 0.35, goalDepth, "white", camera);

		// Players
		drawPlayers(canvas, ctx, data.team1, goalDepth, "red", camera);
		drawPlayers(canvas, ctx, data.team2, goalDepth, "blue", camera);

		// Heatmaps
		drawScores(canvas, ctx, data.team1, goalDepth, camera);
	}, [data, camera]);

	return <canvas ref={canvasRef} style={{ backgroundColor: "#006400"}} />;
};

// --------------------
// Helpers
// --------------------
function perspectivePoint(
    x: number,
    y: number,
    canvas: HTMLCanvasElement,
    camera: { x: number; y: number }
): Translation2d {
    // squash height
    let newY = y * HEIGHT_SCALE + (1 - HEIGHT_SCALE) * canvas.height / 2;

    // tilt upwards
    newY += TILT_Y;

    // normal FIFA tapering
    const scale = getScale(canvas, y);

    // screen center
    const cx = canvas.width / 2;
    const cy = canvas.height / 2;

    // apply zoom around center
    const dx = (x - cx) * scale * ZOOM;
    const dy = (newY - cy) * ZOOM;

    return {
        x: cx + dx - camera.x * ZOOM,
        y: cy + dy + camera.y * ZOOM
    };
}

function getScale(
    canvas: HTMLCanvasElement,
    y: number,
) {
  // squash height
  y = y * HEIGHT_SCALE + (1 - HEIGHT_SCALE) * canvas.height / 2;

  // tilt upwards
  y += TILT_Y;

  // 0 = top of screen, 1 = bottom
  const t = y / canvas.height;
  return TOP_SCALE + (BOTTOM_SCALE - TOP_SCALE) * t;
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
	canvas: HTMLCanvasElement,
	ctx: CanvasRenderingContext2D,
	position: Translation2d,
	radius: number,
  goalDepth: number,
	camera: { x: number; y: number }
) {
	drawCircle(canvas, ctx, position, radius, goalDepth, "white", camera);
}

// Players & Ball
function convert(
	canvas: HTMLCanvasElement,
	position: { x: number; y: number },
	goalDepth: number,
	camera: { x: number; y: number }
): Translation2d {
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
    color: string,
    camera: { x: number; y: number }
) {
    if (!team) return;

    const baseSize = 0.75;

    team.players.forEach((p, i) => {
        const {pos, radiusX, radiusY} = fillCircle(canvas, ctx, p.position, baseSize, goalDepth, color, camera);

        // highlight selected player
        if (i === team.teamStrategy.chosenPlayerIndex) {
            ctx.strokeStyle = "yellow";
            ctx.lineWidth = 2;
            ctx.beginPath();
            ctx.ellipse(pos.x, pos.y, radiusX, radiusY, 0, 0, Math.PI * 2);
            ctx.stroke();
        }
    });
}

function fillCircle(
  canvas: HTMLCanvasElement,
  ctx: CanvasRenderingContext2D,
  position: Translation2d,
  radius: number,
  goalDepth: number,
  color: string,
  camera: { x: number; y: number }
) {
    radius *= (canvas.width - (2 * goalDepth)) / pitchWidthUnits;
    const pos = convert(canvas, position, goalDepth, camera);
    // const scale = getScale(canvas, p.position.y);

    // perspective scaling for ellipse
    const radiusX = radius * ZOOM;
    const radiusY = radiusX * 0.5; // flatten for FIFA look

    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.ellipse(pos.x, pos.y, radiusX, radiusY, 0, 0, Math.PI * 2);
    ctx.fill();

    return {pos, radiusX, radiusY};
}

function drawCircle(
  canvas: HTMLCanvasElement,
  ctx: CanvasRenderingContext2D,
  position: Translation2d,
  radius: number,
  goalDepth: number,
  color: string,
  camera: { x: number; y: number }
) {
    radius *= (canvas.width - (2 * goalDepth)) / pitchWidthUnits;
    const pos = convert(canvas, position, goalDepth, camera);
    // const scale = getScale(canvas, p.position.y);

    // perspective scaling for ellipse
    const radiusX = radius * ZOOM;
    const radiusY = radiusX * 0.55; // flatten for FIFA look

    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.ellipse(pos.x, pos.y, radiusX, radiusY, 0, 0, Math.PI * 2);
    ctx.stroke();

    return {pos, radiusX, radiusY};
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
    const pos = entry.key;
		const color = scoreToColor(entry.value, -120, max);
		const sizeX = pitchWidthUnits / 40;
		const sizeY = pitchHeightUnits / 40;
		ctx.globalAlpha = 0.3;
    fillPoly(ctx, color, [
      convert(canvas, { x: pos.x - sizeX / 2, y: pos.y - sizeY / 2 }, goalDepth, camera),
      convert(canvas, { x: pos.x - sizeX / 2, y: pos.y + sizeY / 2 }, goalDepth, camera),
      convert(canvas, { x: pos.x + sizeX / 2, y: pos.y + sizeY / 2 }, goalDepth, camera),
      convert(canvas, { x: pos.x + sizeX / 2, y: pos.y - sizeY / 2 }, goalDepth, camera),
    ]);
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

function fillPoly(ctx: CanvasRenderingContext2D, color: string, points: Translation2d[]) {
    if (!points || points.length < 3) return; // Need at least 3 points

    ctx.beginPath();
    ctx.moveTo(points[0].x, points[0].y);

    for (let i = 1; i < points.length; i++) {
        ctx.lineTo(points[i].x, points[i].y);
    }

    ctx.closePath();
    ctx.fillStyle = color;
    ctx.fill();
}

export default GameRenderer3D;
