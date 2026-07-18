# Python Audio Analysis Service

## Requirements

- Python 3.10+
- `ffmpeg` available on `PATH`

## Install

```bash
pip install -r requirements.txt
```

## Run

```bash
uvicorn app:APP --host 0.0.0.0 --port 9008
```

## API

- `GET /health`
- `POST /analyze`

Example request:

```json
{
  "file_path": "E:/temp/example.mp3",
  "file_name": "example.mp3",
  "title": "Example",
  "artist": "Artist",
  "album": "Album",
  "genre": "Pop",
  "lyrics": "",
  "source_kind": "upload"
}
```
