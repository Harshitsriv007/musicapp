import type { Song } from '../api/types';
import { useLikes } from '../hooks/LikesContext';
import { HeartIcon } from './Icons';

/** Like every song of a folder, or unlike them all when they are already liked. */
export function LikeAllButton({ songs }: { songs: Song[] }) {
  const { isLiked, setLikedMany } = useLikes();
  if (songs.length === 0) return null;
  const notLiked = songs.filter((s) => !isLiked(s)).length;
  const allLiked = notLiked === 0;
  return (
    <button
      type="button"
      className={`btn btn--ghost${allLiked ? ' btn--liked' : ''}`}
      aria-pressed={allLiked}
      onClick={() => void setLikedMany(songs, !allLiked)}
    >
      <HeartIcon filled={allLiked} width={16} height={16} />
      {allLiked ? 'Unlike all' : `Like all (${notLiked})`}
    </button>
  );
}
