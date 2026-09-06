import "dotenv/config";
import { spawn } from "child_process";
import { randomUUID } from "crypto";
import http from "http";

const token = process.env.NGROK_AUTHTOKEN;
if (!token) {
  console.error("NGROK_AUTHTOKEN missing");
  process.exit(1);
}

const port = Number(process.env.PORT) || 4000;
const NGROK_BIN = process.env.NGROK_BIN || "/usr/local/bin/ngrok";
const API = "http://127.0.0.1:4040";
const NAME = "civic-" + randomUUID().slice(0, 8);

const procs = [];
let killed = false;

function spawnLogged(name, cmd, args) {
  const p = spawn(cmd, args, { stdio: ["ignore", "pipe", "pipe"] });
  p.stdout.on("data", (d) => process.stdout.write(`[${name}] ${d}`));
  p.stderr.on("data", (d) => process.stderr.write(`[${name}] ${d}`));
  p.on("exit", (code) => {
    if (!killed) {
      console.error(`[${name}] exited with code ${code}`);
      cleanup(code ?? 1);
    }
  });
  procs.push(p);
  return p;
}

function cleanup(code = 0) {
  if (killed) return;
  killed = true;
  for (const p of procs) {
    try { p.kill("SIGTERM"); } catch {}
  }
  setTimeout(() => process.exit(code), 500);
}
process.on("SIGINT", () => cleanup(0));
process.on("SIGTERM", () => cleanup(0));

async function waitForBackend() {
  const start = Date.now();
  while (Date.now() - start < 60000) {
    try {
      await new Promise((resolve, reject) => {
        const req = http.get(`http://127.0.0.1:${port}/`, (res) => {
          res.resume();
          resolve();
        });
        req.on("error", reject);
        req.setTimeout(1000, () => req.destroy(new Error("timeout")));
      });
      return;
    } catch {}
    await new Promise((r) => setTimeout(r, 500));
  }
  throw new Error("Backend did not start within 60s");
}

function waitFor(cond, label, ms = 30000) {
  return new Promise((resolve, reject) => {
    const start = Date.now();
    const tick = () => {
      if (cond()) return resolve();
      if (Date.now() - start > ms) return reject(new Error(`Timeout: ${label}`));
      setTimeout(tick, 100);
    };
    tick();
  });
}

console.log("[orchestrator] starting backend on port", port);
spawnLogged("backend", "node", ["server.js"]);

await waitForBackend();
console.log("[orchestrator] backend is up");

console.log("[orchestrator] starting ngrok");
const ngrok = spawnLogged("ngrok", NGROK_BIN, [
  "start",
  "--none",
  "--log=stdout",
  `--authtoken=${token}`,
]);

let apiReady = false;
let sessionReady = false;
ngrok.stdout.on("data", (d) => {
  const t = d.toString();
  if (!apiReady && /starting web service.*addr=127\.0\.0\.1:4040/.test(t)) {
    apiReady = true;
  }
  if (!sessionReady && /tunnel session started/.test(t)) {
    sessionReady = true;
  }
});

await waitFor(() => apiReady, "ngrok API");
await waitFor(() => sessionReady, "ngrok session");

const res = await fetch(`${API}/api/tunnels`, {
  method: "POST",
  headers: { "Content-Type": "application/json", "ngrok-version": "2" },
  body: JSON.stringify({ name: NAME, addr: port, proto: "http" }),
});

if (!res.ok) {
  const body = await res.text();
  throw new Error(`Tunnel creation failed: ${res.status} ${body}`);
}

const tunnel = await res.json();
console.log(`[orchestrator] NGROK_URL=${tunnel.public_url}`);
console.log(`[orchestrator] public URL: ${tunnel.public_url}`);

setInterval(() => {}, 1 << 30);
