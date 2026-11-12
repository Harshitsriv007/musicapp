export type DownloadStatus = 'NONE' | 'PENDING' | 'DOWNLOADING' | 'DOWNLOADED' | 'FAILED';

export interface Song {
  id: number;
  title: string;
  artist: string | null;
  album: string | null;
  /** Movie (or album) the song belongs to, when known. */
  movie: string | null;
  genre: string | null;
  imageUrl: string | null;
  streamUrl: string;
  durationSeconds: number | null;
  /** Upload date on the music API. */
  releaseDate: string | null;
  /** When the song first came out (MusicBrainz), if known. Decade folders use this. */
  originalReleaseDate: string | null;
  source: string;
  addedAt: string | null;
  liked: boolean;
  /** DOWNLOADED = audio is stored in our MySQL and streams from our API. */
  downloadStatus: DownloadStatus;
  /** Track page on the provider (Jamendo) – its terms require crediting and linking back. */
  shareUrl: string | null;
  licenseUrl: string | null;
  /** False when the provider forbids storing the audio: no download button. */
  downloadAllowed: boolean;
}

export type FolderType = 'decades' | 'artists' | 'movies';
/** Query parameter used to list the songs inside a folder. */
export type FolderFilter = 'decade' | 'artist' | 'movie';

export interface Folder {
  key: string;
  title: string;
  songCount: number;
  imageUrl: string | null;
}

export interface SearchResult {
  query: string;
  songs: Song[];
  artists: Folder[];
  movies: Folder[];
}

export interface SongQuery {
  downloaded?: boolean;
  /** 'latest' or a decade start year such as '1990' */
  decade?: string;
  artist?: string;
  movie?: string;
}

export interface DownloadStatusEntry {
  songId: number;
  downloadStatus: DownloadStatus;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface HistoryEntry {
  id: number;
  song: Song;
  playedAt: string;
}

export interface LikedEntry {
  song: Song;
  likedAt: string;
}

export interface AuthResponse {
  token: string;
  username: string;
  /** ISO timestamp; after it the listener must log in again. */
  expiresAt: string;
}
