import React, { useState, useEffect } from "react";

const API_URL = "http://localhost:8080/api/videos";

function App() {
  const [videos, setVideos] = useState([]);
  const [selected, setSelected] = useState(null);
  const [file, setFile] = useState(null);
  const [title, setTitle] = useState("");

  useEffect(() => {
    fetch(API_URL)
      .then(res => res.json())
      .then(setVideos);
  }, []);

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!file || !title) return;
    const formData = new FormData();
    formData.append("file", file);
    formData.append("title", title);

    await fetch(`${API_URL}/upload`, {
      method: "POST",
      body: formData,
    });
    setFile(null);
    setTitle("");
    fetch(API_URL)
      .then(res => res.json())
      .then(setVideos);
  };

  return (
    <div style={{ maxWidth: 600, margin: "auto" }}>
      <h1>Video Streaming App</h1>
      <form onSubmit={handleUpload}>
        <input
          type="text"
          placeholder="Video title"
          value={title}
          onChange={e => setTitle(e.target.value)}
          required
        />
        <input
          type="file"
          accept="video/mp4"
          onChange={e => setFile(e.target.files[0])}
          required
        />
        <button type="submit">Upload</button>
      </form>
      <h2>Available Videos</h2>
      <ul>
        {videos.map(v => (
          <li key={v.id}>
            <button onClick={() => setSelected(v)}>{v.title}</button>
          </li>
        ))}
      </ul>
      {selected && (
        <div>
          <h3>{selected.title}</h3>
          <video
            width="100%"
            height="360"
            controls
            src={`${API_URL}/stream/${selected.id}`}
          />
        </div>
      )}
    </div>
  );
}

export default App; 