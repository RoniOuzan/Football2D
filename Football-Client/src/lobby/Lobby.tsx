import type Client from "../client/Client";
import type { LobbyData } from "../client/jsons/lobbyTypes";

interface LobbyProps {
  client: Client;
  data: LobbyData;
}

function Lobby({ client, data }: LobbyProps) {
  const waitingGames = data.waitingGames;
  const runningGames = data.games;

  return (
    <div
      style={{
        width: "100%",
        height: "100%",
        background: "#0b0b0b",
        color: "white",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        gap: 28,
        padding: 20,
      }}
    >
      <h1 style={{ margin: 0 }}>⚽ Football Lobby</h1>

      {/* CREATE GAME */}
      <button
        style={{
          padding: "14px 28px",
          fontSize: 18,
          cursor: "pointer",
        }}
        onClick={() => client.createGame()}
      >
        Create Game
      </button>

      <div
        style={{
          display: "flex",
          gap: 40,
          marginTop: 20,
          flexWrap: "wrap",
          justifyContent: "center",
        }}
      >
        {/* WAITING GAMES */}
        <Section title="Waiting Games">
          {waitingGames.length ? (
            waitingGames.map((game) => (
              <GameRow key={game.uuid}>
                <span>Game {game.uuid.slice(0, 6)}</span>
                <button onClick={() => client.joinGame(game.uuid)}>Join</button>
              </GameRow>
            ))
          ) : (
            <Empty>No waiting games</Empty>
          )}
        </Section>

        {/* RUNNING GAMES */}
        <Section title="Running Games">
          {runningGames.length ? (
            runningGames.map((game) => (
              <GameRow key={game.uuid} disabled>
                <span>
                  {game.score1} : {game.score2}
                </span>
                {/* <span>{Math.floor(game.time)}s</span> */}
              </GameRow>
            ))
          ) : (
            <Empty>No running games</Empty>
          )}
        </Section>
      </div>
    </div>
  );
}

export default Lobby;

/* ---------- UI Helpers ---------- */

function Section({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div style={{ width: 260 }}>
      <h3 style={{ marginBottom: 10 }}>{title}</h3>
      {children}
    </div>
  );
}

function GameRow({
  children,
  disabled,
}: {
  children: React.ReactNode;
  disabled?: boolean;
}) {
  return (
    <div
      style={{
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        padding: "8px 0",
        opacity: disabled ? 0.6 : 1,
      }}
    >
      {children}
    </div>
  );
}

function Empty({ children }: { children: React.ReactNode }) {
  return <div style={{ opacity: 0.6 }}>{children}</div>;
}
