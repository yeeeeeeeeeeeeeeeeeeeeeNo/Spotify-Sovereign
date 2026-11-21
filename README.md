# Spotify Sovereign v3.0 — Atmos Studio

100% independent Android music streaming client built in Kotlin + Jetpack Compose + AndroidX Media3, with zero third-party extractor dependencies.

## Key Features
- **Bounded-Chunk HTTP/2 Streaming Engine (`BoundedChunkOkHttpDataSource`)**: Seamless 512 KB range-chunk streaming + 512 MB LRU `SimpleCache` + predictive next-track pre-buffering (zero 0:00 stalls, zero 30s cutoffs).
- **32-bit Sovereign Studio Mastering & Dolby Atmos DSP (`SovereignDspEngine`)**: Hardware `DynamicsProcessing` 10-Band EQ (`31 Hz → 16 kHz`) + 5-Band Multiband Compressor + Brickwall Soft-Knee Limiter + Dolby Atmos 3D Spatializer + Sub-Bass Punch + Micro-Clarity Exciter across 8 live sound profiles + Custom 10-Band Graphic EQ.
- **Next-Gen Player UI**: Living 4-orb Ambient Aurora Mesh backdrop, swipe-to-skip interactive artwork stage (*Hero Squircle*, *Spinning Vinyl Disc*, *Cinematic Full*), animated Squiggly Wave progress scrubber with haptic feedback, morphing Play/Pause controls, and particle burst likes.
- **Cinema Synced Lyrics**: Optical depth-of-field blur (Android 12+), progressive luminous karaoke sweep, interlude breather dots, tap-to-seek, and per-track live sync offset tuner persisted in SQLite.
- **On-Device SQLite AI Taste Brain**: Behavioral telemetry (completion %, fast-skip decay, Markov transitions, time-of-day vibe buckets, 1-tap public Spotify playlist link importer).
- **Theme Studio**: 6 live themes (*Spotify Obsidian*, *AMOLED Pitch Black*, *Midnight Sapphire*, *Crimson Velvet*, *Royal Amethyst*, *Dynamic Album Aura*).

## Build
```bash
export JAVA_HOME="/path/to/jdk-17"
./gradlew assembleRelease
```
