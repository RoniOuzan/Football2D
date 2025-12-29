import type Client from "../client/Client";
import type { LobbyData } from "../client/jsons/lobbyTypes";
import "./lobby.css";

interface LobbyProps {
  client: Client;
  data: LobbyData;
}

function Lobby({ client, data }: LobbyProps) {
  const waitingGames = data.waitingGames;
  const runningGames = data.games;

  return (
    <div className="lobby-container">
      <header className="lobby-header">
        <h1 className="lobby-title">
          <span>Football</span> Lobby
        </h1>
      </header>

      <button className="create-btn" onClick={() => client.createGame()}>
        + Create Match
      </button>

      <div className="lobby-grid">
        {/* WAITING GAMES */}
        <div className="lobby-section">
          <h3 className="section-title">Available Lobbies</h3>
          {waitingGames.length ? (
            waitingGames.map((game) => (
              <div key={game.uuid} className="game-row">
                <div style={{ display: "flex", flexDirection: "column" }}>
                  <small style={{ color: "var(--accent-green)", fontSize: "10px" }}>WAITING</small>
                  <span style={{ fontWeight: "bold" }}>ID: {game.uuid.slice(0, 6)}</span>
                </div>
                <button className="join-btn" onClick={() => client.joinGame(game.uuid)}>
                  JOIN
                </button>
              </div>
            ))
          ) : (
            <div className="empty-msg">No matches waiting...</div>
          )}
        </div>

        {/* RUNNING GAMES */}
        <div className="lobby-section">
          <h3 className="section-title">Live Now</h3>
          {runningGames.length ? (
            runningGames.map((game) => (
              <div key={game.uuid} className="game-row live">
                <div className="score-display">
                  {game.score1} <span>:</span> {game.score2}
                </div>
                <button className="spec-btn" onClick={() => client.spectateGame(game.uuid)}>
                  SPECTATE
                </button>
              </div>
            ))
          ) : (
            <div className="empty-msg">No live matches...</div>
          )}
        </div>
      </div>
    </div>
  );
}

export default Lobby;