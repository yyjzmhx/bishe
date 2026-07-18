from __future__ import annotations

import math
import os
from typing import Any

import librosa
import numpy as np
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

APP = FastAPI(title="music-audio-analysis-service", version="1.0.0")

VECTOR_LABELS = [
    "能量强度",
    "节奏活跃度",
    "音色明亮度",
    "动态起伏",
    "纹理复杂度",
]


class AnalyzeRequest(BaseModel):
    file_path: str = Field(..., description="Absolute local path to a readable audio file")
    file_name: str | None = None
    title: str | None = None
    artist: str | None = None
    album: str | None = None
    genre: str | None = None
    lyrics: str | None = None
    source_kind: str | None = None


def clamp(value: float, low: float = 0.0, high: float = 1.0) -> float:
    return max(low, min(high, value))


def safe_text(value: str | None) -> str | None:
    if value is None:
        return None
    stripped = value.strip()
    return stripped if stripped else None


def normalize_feature(values: np.ndarray) -> list[float]:
    return [round(clamp(float(v)), 4) for v in values.tolist()]


def mean_or_zero(value: np.ndarray) -> float:
    return float(np.mean(value)) if value.size else 0.0


def infer_style(tempo: float, brightness: float, energy: float, chroma_var: float) -> str:
    if tempo >= 128 and brightness >= 0.58:
        return "电子/流行"
    if energy >= 0.72 and brightness < 0.45:
        return "摇滚/说唱"
    if energy < 0.40 and tempo < 95:
        return "抒情/氛围"
    if chroma_var >= 0.20:
        return "旋律流行"
    return "流行"


def infer_emotion(energy: float, brightness: float, dynamic_range: float) -> str:
    if energy >= 0.75 and brightness >= 0.55:
        return "兴奋/积极"
    if energy <= 0.35 and brightness <= 0.45:
        return "平静/沉浸"
    if dynamic_range >= 0.55:
        return "抒情/张力"
    return "舒展/自然"


def infer_rhythm(tempo: float, onset_strength: float) -> str:
    if tempo >= 128 or onset_strength >= 0.65:
        return "节奏推进明显"
    if tempo >= 100:
        return "中等律动"
    return "节奏舒缓"


def infer_atmosphere(energy: float, brightness: float, spectral_flatness: float) -> str:
    if energy < 0.35 and brightness < 0.45:
        return "安静沉浸"
    if brightness >= 0.60 and energy >= 0.60:
        return "明亮外放"
    if spectral_flatness >= 0.25:
        return "层次递进"
    return "轻盈流畅"


def infer_instruments(brightness: float, percussive_ratio: float, vocal_hint: bool) -> list[str]:
    instruments: list[str] = []
    if vocal_hint:
        instruments.append("人声线条")
    instruments.append("明亮合成器" if brightness >= 0.58 else "柔和铺底")
    if percussive_ratio >= 0.42:
        instruments.append("节奏打击")
    else:
        instruments.append("旋律层次")
    return instruments


@APP.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@APP.post("/analyze")
def analyze_audio(request: AnalyzeRequest) -> dict[str, Any]:
    file_path = request.file_path
    if not os.path.isfile(file_path):
        raise HTTPException(status_code=400, detail=f"Audio file not found: {file_path}")

    try:
        signal, sample_rate = librosa.load(file_path, sr=22050, mono=True)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"Failed to decode audio: {exc}") from exc

    if signal.size < 2048:
        raise HTTPException(status_code=400, detail="Audio signal is too short for analysis")

    duration_seconds = float(librosa.get_duration(y=signal, sr=sample_rate))
    rms = librosa.feature.rms(y=signal)[0]
    centroid = librosa.feature.spectral_centroid(y=signal, sr=sample_rate)[0]
    rolloff = librosa.feature.spectral_rolloff(y=signal, sr=sample_rate)[0]
    flatness = librosa.feature.spectral_flatness(y=signal)[0]
    zcr = librosa.feature.zero_crossing_rate(y=signal)[0]
    onset_env = librosa.onset.onset_strength(y=signal, sr=sample_rate)
    tempo = float(librosa.feature.tempo(onset_envelope=onset_env, sr=sample_rate)[0])
    harmonic, percussive = librosa.effects.hpss(signal)
    chroma = librosa.feature.chroma_stft(y=harmonic, sr=sample_rate)
    mfcc = librosa.feature.mfcc(y=signal, sr=sample_rate, n_mfcc=13)

    energy = clamp(mean_or_zero(rms) / 0.25)
    rhythm_activity = clamp(((tempo - 60.0) / 120.0) * 0.6 + mean_or_zero(onset_env) * 0.4)
    brightness = clamp((mean_or_zero(centroid) / 5000.0) * 0.7 + (mean_or_zero(rolloff) / 8000.0) * 0.3)
    dynamic_range = clamp(float(np.std(rms)) / 0.12)
    texture_complexity = clamp(float(np.std(mfcc)) / 140.0 + mean_or_zero(flatness) * 0.35 + mean_or_zero(zcr) * 0.15)

    vector = normalize_feature(
        np.array([energy, rhythm_activity, brightness, dynamic_range, texture_complexity], dtype=np.float32)
    )

    spectral_flatness = clamp(mean_or_zero(flatness))
    onset_strength = clamp(mean_or_zero(onset_env) / 8.0)
    percussive_ratio = clamp(float(np.mean(np.abs(percussive))) / (float(np.mean(np.abs(signal))) + 1e-6))
    chroma_var = clamp(float(np.std(chroma)))
    vocal_hint = bool(safe_text(request.lyrics))

    style = safe_text(request.genre) or infer_style(tempo, brightness, energy, chroma_var)
    emotion = infer_emotion(energy, brightness, dynamic_range)
    rhythm = infer_rhythm(tempo, onset_strength)
    atmosphere = infer_atmosphere(energy, brightness, spectral_flatness)
    instruments = infer_instruments(brightness, percussive_ratio, vocal_hint)

    bpm = round(tempo, 2)
    key_index = int(np.argmax(np.mean(chroma, axis=1)))
    key_names = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]
    detected_key = key_names[key_index]

    description = (
        f"{safe_text(request.title) or safe_text(request.file_name) or '该音频'}"
        f"呈现出{style}取向，整体情绪偏{emotion}，节奏表现为{rhythm}，氛围上更接近{atmosphere}。"
    )

    keywords = [
        style,
        emotion,
        rhythm,
        atmosphere,
        f"BPM {int(round(bpm))}",
        f"调性 {detected_key}",
    ]

    technical_summary = (
        f"模式=python-librosa, 时长={round(duration_seconds, 2)}秒, 采样率={sample_rate}Hz, "
        f"节拍={bpm} BPM, 调性={detected_key}, 均方根={round(mean_or_zero(rms), 4)}, "
        f"频谱质心={round(mean_or_zero(centroid), 2)}, 频谱滚降={round(mean_or_zero(rolloff), 2)}"
    )

    return {
        "style": style,
        "emotion": emotion,
        "rhythm": rhythm,
        "instruments": instruments,
        "atmosphere": atmosphere,
        "description": description,
        "keywords": keywords,
        "vector": vector,
        "vectorLabels": VECTOR_LABELS,
        "technicalSummary": technical_summary,
        "extractionMode": "python-librosa",
        "durationSeconds": int(round(duration_seconds)),
        "sampleRate": sample_rate,
        "bitRateKbps": None,
        "channels": 1,
        "formatName": os.path.splitext(file_path)[1].replace(".", "").lower() or None,
        "codecName": "ffmpeg/librosa",
        "tagTitle": safe_text(request.title),
        "tagArtist": safe_text(request.artist),
        "tagAlbum": safe_text(request.album),
        "tagGenre": safe_text(request.genre),
        "tagComment": None,
        "aiEnhanced": False,
        "extra": {
            "tempoBpm": bpm,
            "detectedKey": detected_key,
            "spectralFlatness": round(spectral_flatness, 4),
            "percussiveRatio": round(percussive_ratio, 4),
            "mfccMean": [round(float(v), 4) for v in np.mean(mfcc, axis=1).tolist()],
        },
    }
