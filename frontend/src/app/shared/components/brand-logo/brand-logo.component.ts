import { IMAGE_LOADER, NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { widthVariantImageLoader } from '@/shared/utils/width-variant-image-loader';

const LOGO_SRC = 'logos/simbolo.webp';
const LOGO_WIDTH = 96;
const LOGO_HEIGHT = 96;

@Component({
  selector: 'app-brand-logo',
  imports: [NgOptimizedImage],
  providers: [{ provide: IMAGE_LOADER, useValue: widthVariantImageLoader }],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './brand-logo.component.html',
})
export class BrandLogoComponent {
  readonly tagline = input('Gestiona tus citas');

  protected readonly logoSrc = LOGO_SRC;
  protected readonly logoWidth = LOGO_WIDTH;
  protected readonly logoHeight = LOGO_HEIGHT;
}
