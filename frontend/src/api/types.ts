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
