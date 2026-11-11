import { useCallback, useEffect, useState } from 'react';

interface DocumentPictureInPicture {
  requestWindow(options?: { width?: number; height?: number }): Promise<Window>;
  window: Window | null;
}

declare global {
  interface Window {
    documentPictureInPicture?: DocumentPictureInPicture;
  }
}

/**
 * Document Picture-in-Picture (Chrome / Edge desktop): opens an always-on-top mini window that
 * stays visible while the main window is minimised or behind other apps. We copy our styles into
 * it and React renders the controls there through a portal.
 */
export function usePictureInPicture() {
  const supported = typeof window !== 'undefined' && !!window.documentPictureInPicture;
  const [pipWindow, setPipWindow] = useState<Window | null>(null);

  const open = useCallback(async () => {
    if (!window.documentPictureInPicture) return;
    const pip = await window.documentPictureInPicture.requestWindow({ width: 340, height: 132 });
    for (const sheet of Array.from(document.styleSheets)) {
      try {
        const style = pip.document.createElement('style');
        style.textContent = Array.from(sheet.cssRules).map((r) => r.cssText).join('\n');
        pip.document.head.appendChild(style);
      } catch {
        if (sheet.href) {
          const link = pip.document.createElement('link');
          link.rel = 'stylesheet';
          link.href = sheet.href;
          pip.document.head.appendChild(link);
        }
      }
    }
    pip.document.title = 'Dhun mini player';
    pip.document.body.className = 'pip-body';
    pip.addEventListener('pagehide', () => setPipWindow(null), { once: true });
    setPipWindow(pip);
  }, []);

  const close = useCallback(() => {
    pipWindow?.close();
    setPipWindow(null);
  }, [pipWindow]);

  useEffect(() => () => pipWindow?.close(), [pipWindow]);

  return { supported, pipWindow, open, close };
}
