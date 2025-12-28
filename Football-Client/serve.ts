import fs from "fs";
import path from "path";
import express from "express";
import https from "https";
import type { Request, Response } from "express";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const distPath = path.join(__dirname, "dist");

// Serve static files
app.use(express.static(distPath));

// SPA fallback — works without path-to-regexp
app.use((req: Request, res: Response) => {
  const indexFile = path.join(distPath, "index.html");
  if (fs.existsSync(indexFile)) {
    res.sendFile(indexFile);
  } else {
    res.status(404).send("Not found");
  }
});

// HTTPS server
const server = https.createServer(
  {
    key: fs.readFileSync(path.join(__dirname, "192.168.1.73+2-key.pem")),
    cert: fs.readFileSync(path.join(__dirname, "192.168.1.73+2.pem")),
  },
  app
);

server.listen(5173, () => {
  console.log("✅ HTTPS server running at https://192.168.1.73:5173");
});
