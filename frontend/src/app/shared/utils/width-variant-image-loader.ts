import { ImageLoaderConfig } from '@angular/common';

/** Maps each requested width to its pre-generated variant, e.g. `logo.webp` -> `logo-192.webp`. */
export function widthVariantImageLoader(config: ImageLoaderConfig): string {
  return config.width ? config.src.replace('.webp', `-${config.width}.webp`) : config.src;
}
