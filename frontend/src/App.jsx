import React, { useState, useEffect, useCallback } from "react";

const API_BASE = process.env.REACT_APP_API_BASE || "http://localhost:8080";

// Centralised fetch wrapper — throws a normalised Error in every failure case.
async function apiFetch(path, options = {}) {
  let res;
  try {
    res = await fetch(`${API_BASE}${path}`, options);
  } catch {
    throw new Error("Cannot reach the server. Check that the backend is running.");
  }

  // Try to parse JSON; fall back gracefully if the body isn't JSON
  let data;
  try {
    data = await res.json();
  } catch {
    throw new Error(`Server returned an unexpected response (HTTP ${res.status}).`);
  }

  if (!res.ok) {
    throw new Error(data?.error || `Request failed (HTTP ${res.status}).`);
  }

  return data;
}

export default function App() {
  const [url, setUrl] = useState("");
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  const [stats, setStats] = useState(null);
  const [statsError, setStatsError] = useState("");
  const [statsLoading, setStatsLoading] = useState(false);

  const [leaderboard, setLeaderboard] = useState([]);
  const [lbError, setLbError] = useState("");
  const [lbLoading, setLbLoading] = useState(false);

  const fetchLeaderboard = useCallback(async () => {
    setLbLoading(true);
    setLbError("");
    try {
      const data = await apiFetch("/api/leaderboard?limit=10");
      setLeaderboard(Array.isArray(data) ? data : []);
    } catch (err) {
      setLbError(err.message);
    } finally {
      setLbLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchLeaderboard();
  }, [fetchLeaderboard]);

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setResult(null);
    setStats(null);
    setStatsError("");
    setCopied(false);

    if (!url.trim()) {
      setError("Enter a URL first.");
      return;
    }

    setLoading(true);
    try {
      const data = await apiFetch("/api/shorten", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ url: url.trim() }),
      });
      setResult(data);
      fetchLeaderboard();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function handleCopy() {
    if (!result) return;
    navigator.clipboard.writeText(result.shortUrl).catch(() => {
      // Clipboard API can be blocked in some browsers/contexts
      setError("Could not copy to clipboard. Please copy the link manually.");
    });
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  }

  async function handleStats() {
    if (!result) return;
    const shortCode = result.shortUrl.split("/").pop();
    setStatsLoading(true);
    setStats(null);
    setStatsError("");
    try {
      const data = await apiFetch(`/api/stats/${shortCode}`);
      setStats(data);
    } catch (err) {
      setStatsError(err.message);
    } finally {
      setStatsLoading(false);
    }
  }

  return (
    <div className="page">
      <header className="header">
        <span className="logo">Snip</span>
        <p className="tagline">Paste a long link. Get a short one.</p>
      </header>

      <form className="card" onSubmit={handleSubmit}>
        <input
          type="text"
          className="url-input"
          placeholder="https://example.com/a/very/long/url"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
        />
        <button type="submit" className="shorten-btn" disabled={loading}>
          {loading ? "Shortening…" : "Shorten"}
        </button>

        {error && <p className="error">{error}</p>}

        {result && (
          <>
            <div className="result">
              <a href={result.shortUrl} target="_blank" rel="noreferrer" className="short-link">
                {result.shortUrl}
              </a>
              <button type="button" className="copy-btn" onClick={handleCopy}>
                {copied ? "Copied!" : "Copy"}
              </button>
              <button type="button" className="stats-btn" onClick={handleStats} disabled={statsLoading}>
                {statsLoading ? "…" : "Stats"}
              </button>
            </div>

            {statsError && <p className="error">{statsError}</p>}

            {stats && !statsError && (
              <div className="stats-box">
                <span className="stats-label">Click count</span>
                <span className="stats-count">{stats.clickCount}</span>
              </div>
            )}
          </>
        )}
      </form>

      <div className="leaderboard">
        <div className="leaderboard-header">
          <span className="leaderboard-title">Top links by clicks</span>
          <button className="leaderboard-refresh" onClick={fetchLeaderboard} disabled={lbLoading}>
            {lbLoading ? "Loading…" : "↻ Refresh"}
          </button>
        </div>

        {lbError && <p className="error">{lbError}</p>}

        {!lbError && leaderboard.length === 0 && !lbLoading && (
          <p className="leaderboard-empty">No links yet — shorten one above!</p>
        )}

        {leaderboard.map((entry, i) => (
          <div className="leaderboard-row" key={entry.shortCode}>
            <span className={`leaderboard-rank ${i < 3 ? "top" : ""}`}>
              {i === 0 ? "🥇" : i === 1 ? "🥈" : i === 2 ? "🥉" : `#${i + 1}`}
            </span>
            <span className="leaderboard-url" title={entry.originalUrl}>
              {entry.originalUrl}
            </span>
            <span className="leaderboard-clicks">{entry.clickCount} clicks</span>
          </div>
        ))}
      </div>

      <footer className="footer">Full persistence — links are kept forever.</footer>
    </div>
  );
}
