import { useState } from 'react';
import { MusicIcon } from './Icons';

export function Artwork({ src, size, alt = '' }: { src: string | null; size: number; alt?: string }) {
  const [failed, setFailed] = useState(false);
  const style = { width: size, height: size };
  if (!src || failed) {
    return (
      <div className="artwork artwork--empty" style={style} aria-hidden={alt ? undefined : true}>
        <MusicIcon width={size * 0.45} height={size * 0.45} />
      </div>
    );
  }
  return (
    <img className="artwork" style={style} src={src} alt={alt} loading="lazy" onError={() => setFailed(true)} />
  );
}
