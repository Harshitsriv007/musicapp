/** Initial of the username on a colour derived from it. */
export function Avatar({ name, size = 32 }: { name: string; size?: number }) {
  let h = 0;
  for (let i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) | 0;
  const hue = Math.abs(h) % 360;
  return (
    <span
      className="avatar"
      aria-hidden="true"
      style={{ width: size, height: size, fontSize: size * 0.44, background: `hsl(${hue} 55% 46%)` }}
    >
      {name.charAt(0).toUpperCase()}
    </span>
  );
}
