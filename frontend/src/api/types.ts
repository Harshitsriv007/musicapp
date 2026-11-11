export type DownloadStatus = 'NONE' | 'PENDING' | 'DOWNLOADING' | 'DOWNLOADED' | 'FAILED';

export interface Song {
  id: number;
  title: string;
  artist: string | null;
  album: string | null;
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
