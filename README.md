# Music Player

Android music player with Markdown-based metadata and PostgreSQL caching.

## Architecture

- **Jetpack Compose** UI with Material Design 3
- **ExoPlayer** for audio playback via `MusicPlayerService`
- **PostgreSQL + Apache AGE** graph extension as a local cache
- **Markdown files** (YAML front matter) as the primary metadata storage, similar to Obsidian

## Data model

| Entity | Description |
|---|---|
| `TrackDocument` | Audio file with metadata (aliases, cover, year, album, creators, related tracks) |
| `AlbumDocument` | Collection of tracks with creators and cover |
| `CreatorDocument` | Artist/group with aliases and listen stats |
| `PlaylistDocument` | User-defined track list |

Relationships (creators, tracklists) are stored in junction tables (`track_creators`, `album_creators`, `album_tracks`, `playlist_tracks`) with an `ord` column to preserve order.

## Project structure

```
app/src/main/java/com/example/musicplayer/
├── data/           # Data models, PostgreSQL, MusicRepository
├── mdreader/       # Markdown parser and writer (YAML front matter)
├── ui/             # Compose screens and components
├── service/        # MusicPlayerService (ExoPlayer, MediaSession)
└── audio/          # ReplayGain loudness normalization
```

## Setup

### Prerequisites

- Android Studio with Kotlin
- PostgreSQL running locally (via Termux or native) on port 5432
- Apache AGE extension installed

### Database

```bash
dropdb -h localhost -U user music_player
createdb -h localhost -U user music_player
```

Tables are created automatically on first run.

### Build

```bash
./gradlew assembleDebug
```

## Features

- Track, album, creator, and playlist management
- Favorites with reorderable track list
- ReplayGain loudness normalization
- Full-text search (pg_trgm)
- Graph queries via Apache AGE
