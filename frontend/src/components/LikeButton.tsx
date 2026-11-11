import type { Song } from '../api/types';
import { useLikes } from '../hooks/LikesContext';
import { HeartIcon } from './Icons';

export function LikeButton({ song, size = 20 }: { song: Song; size?: number }) {
  const { isLiked, toggleLike } = useLikes();
  const liked = isLiked(song);
  return (
    <button
      type="button"
      className={`icon-btn like-btn${liked ? ' like-btn--on' : ''}`}
      aria-label={liked ? `Unlike ${song.title}` : `Like ${song.title}`}
      aria-pressed={liked}
      onClick={(e) => {
        e.stopPropagation();
        void toggleLike(song);
      }}
    >
      <HeartIcon filled={liked} width={size} height={size} />
    </button>
  );
}
