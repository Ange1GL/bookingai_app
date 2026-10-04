import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';
import { MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';
import { credentialsInterceptor } from './core/interceptor/credentials.interceptor';
import { errorInterceptor } from './core/interceptor/error.interceptor';
import { loaderInterceptor } from './core/interceptor/loader.interceptor';
import { refreshInterceptor } from './core/interceptor/refresh.interceptor';
import { routes } from './app.routes';

const VioletPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '{violet.50}',
      100: '{violet.100}',
      200: '{violet.200}',
      300: '{violet.300}',
      400: '{violet.400}',
      500: '{violet.500}',
      600: '{violet.600}',
      700: '{violet.700}',
      800: '{violet.800}',
      900: '{violet.900}',
      950: '{violet.950}',
    },
  },
});

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withInMemoryScrolling({ scrollPositionRestoration: 'enabled' })),
    // Order matters: `refresh` is innermost so recovered 401s never reach `error`.
    provideHttpClient(withFetch(), withInterceptors([credentialsInterceptor, loaderInterceptor, errorInterceptor, refreshInterceptor])),
    provideAnimationsAsync(),
    providePrimeNG({ theme: { preset: VioletPreset, options: { darkModeSelector: '.app-dark' } } }),
    MessageService,
  ],
};
