import { Injectable } from '@angular/core';

/** Copies text to the clipboard; falls back to `execCommand` where the async API is unavailable (plain HTTP, old WebViews). */
@Injectable({ providedIn: 'root' })
export class ClipboardService {
  async copy(text: string): Promise<boolean> {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch {
      return this.copyWithSelection(text);
    }
  }

  private copyWithSelection(text: string): boolean {
    const area = document.createElement('textarea');
    area.value = text;
    area.setAttribute('readonly', '');
    area.style.position = 'fixed';
    area.style.opacity = '0';
    document.body.appendChild(area);
    area.select();
    try {
      return document.execCommand('copy');
    } catch {
      return false;
    } finally {
      area.remove();
    }
  }
}
