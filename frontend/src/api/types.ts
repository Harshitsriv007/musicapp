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
  releaseDate: string | null;
  source: string;
  addedAt: string | null;
  liked: boolean;
  /** DOWNLOADED = audio is stored in our MySQL and streams from our API. */
  downloadStatus: DownloadStatus;
}

export type FolderType = 'years' | 'artists' | 'movies';
/** Query parameter used to list the songs inside a folder. */
export type FolderFilter = 'year' | 'artist' | 'movie';

export interface Folder {
  key: string;
  title: string;
  songCount: number;
  imageUrl: string | null;
}

export interface SongQuery {
  downloaded?: boolean;
  year?: string;
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
