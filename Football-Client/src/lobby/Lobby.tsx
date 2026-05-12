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
      <div className="lobby-ambient" aria-hidden="true">
        <span className="orb orb-1" />
        <span className="orb orb-2" />
        <span className="orb orb-3" />
      </div>

      <header className="lobby-header">
        <div className="lobby-banner">
          <h1 className="lobby-title">
            <span>Football</span> Arena
          </h1>
          <p className="lobby-subtitle">
            Jump into live matches, create a new challenge, or watch the action unfold in real time.
          </p>
        </div>
      </header>

      <div className="action-panel">
        <button className="create-btn create-new" onClick={() => client.createGame()}>
          + Create Match
        </button>
        <button className="create-btn bot-btn" onClick={() => client.createBotMatch()}>
          + Player vs Bot
        </button>
        <button className="create-btn bot-vs-bot-btn" onClick={() => client.createBotVsBotMatch()}>
          + Bot vs Bot
        </button>
      </div>

      <div className="lobby-grid">
        {/* WAITING GAMES */}
        <div className="lobby-section">
          <div className="section-header">
            <h3 className="section-title">Available Lobbies</h3>
            <span className="section-note">Quick join a waiting match</span>
          </div>
          {waitingGames.length ? (
            waitingGames.map((game) => (
              <div key={game.uuid} className="game-card">
                <div className="game-info">
                  <span className="status-pill waiting">WAITING</span>
                  <span className="game-id">ID {game.uuid.slice(0, 6)}</span>
                </div>
                <button className="action-btn join-btn" onClick={() => client.joinGame(game.uuid)}>
                  JOIN
                </button>
              </div>
            ))
          ) : (
            <div className="empty-msg">No matches waiting... create your own arena.</div>
          )}
        </div>

        {/* RUNNING GAMES */}
        <div className="lobby-section">
          <div className="section-header">
            <h3 className="section-title">Live Now</h3>
            <span className="section-note">Spectate the hottest games</span>
          </div>
          {runningGames.length ? (
            runningGames.map((game) => (
              <div key={game.uuid} className="game-card live">
                <div className="game-info">
                  <span className="status-pill live-pill">LIVE</span>
                  <div className="score-display">
                    {game.score1} <span>:</span> {game.score2}
                  </div>
                </div>
                <button className="action-btn spec-btn" onClick={() => client.spectateGame(game.uuid)}>
                  SPECTATE
                </button>
              </div>
            ))
          ) : (
            <div className="empty-msg">No live matches... stay tuned.</div>
          )}
        </div>
      </div>
    </div>
  );
}

export default Lobby;