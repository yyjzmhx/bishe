# Python Audio Analysis Service

## Overview

This project now supports an external Python audio analysis service.

- Java keeps upload, storage, recommendation, and frontend delivery.
- Python handles audio decoding and feature extraction.
- Java calls Python and stores the returned feature JSON.

## Local Run

1. Install Python 3.10+ and `ffmpeg`
2. Install dependencies

```bash
cd python-audio-service
pip install -r requirements.txt
```

3. Start the service

```bash
uvicorn app:APP --host 0.0.0.0 --port 9008
```

4. Start the Java backend

The backend default config is:

```yaml
audio.analysis.python.base-url: http://127.0.0.1:9008
```

## Docker Compose

`docker-compose.yml` includes an `audio-analysis` service.

After startup, the endpoint is:

```text
http://localhost:9008/analyze
```

## Notes

- The Python service expects a readable local file path.
- Java downloads the uploaded object from storage to a temporary local file and then calls Python.
- If the Python service is unavailable, Java falls back to the local in-process extractor.
