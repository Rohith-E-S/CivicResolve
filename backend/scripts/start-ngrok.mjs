import "dotenv/config";
import { spawn } from "child_process";
import { randomUUID } from "crypto";

const token = process.env.NGROK_AUTHTOKEN;
if (!token) {
  console.error("NGROK_AUTHTOKEN missing in backend/.env");
  process.exit(1);
}

const port = Number(process.env.PORT) || 4000;
const NGROK_BIN = "/home/rohith/.local/bin/ngrok";
const API = "http://127.0.0.1:4040";
const NAME = "civic-" + randomUUID().slice(0, 8);

const child = spawn(
  NGROK_BIN,
  ["start", "--none", "--log=stdout", `--authtoken=${token}`],
  { stdio: ["ignore", "pipe", "pipe"] }
);

let killed = false;
const cleanup = () => {
  if (killed) return;
  killed = true;
  child.kill();
  process.exit(0);
};
process.on("SIGINT", cleanup);
process.on("SIGTERM", cleanup);

let sessionReady = false;
let apiReady = false;

child.stdout.on("data", (d) => {
  const text = d.toString();
  process.stdout.write(text);
  if (!apiReady && /starting web service.*addr=127\.0\.0\.1:4040/.test(text)) {
    apiReady = true;
  }
  if (!sessionReady && /tunnel session started/.test(text)) {
    sessionReady = true;
  }
});

child.stderr.on("data", (d) => process.stderr.write(d));
child.on("exit", (code) => {
  if (!killed) {
    console.error(`ngrok exited with code ${code}`);
    process.exit(code ?? 1);
  }
});

async function waitFor(fn, label, timeoutMs = 20000) {
  const start = Date.now();
  while (!fn() && Date.now() - start < timeoutMs) {
    await new Promise((r) => setTimeout(r, 100));
  }
  if (!fn()) throw new Error(`Timeout waiting for: ${label}`);
}

await waitFor(() => apiReady, "ngrok API ready (4040)");
await waitFor(() => sessionReady, "ngrok session ready");

const res = await fetch(`${API}/api/tunnels`, {
  method: "POST",
  headers: { "Content-Type": "application/json", "ngrok-version": "2" },
  body: JSON.stringify({
    name: NAME,
    addr: port,
    proto: "http",
  }),
});

if (!res.ok) {
  const body = await res.text();
  throw new Error(`POST /api/tunnels failed: ${res.status} ${body}`);
}

const tunnel = await res.json();
console.log("\nNGROK_URL=" + tunnel.public_url);
console.log(tunnel.public_url);

setInterval(() => {}, 1 << 30);
