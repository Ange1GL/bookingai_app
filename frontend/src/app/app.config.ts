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

const BluePreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '{blue.50}',
      100: '{blue.100}',
      200: '{blue.200}',
      300: '{blue.300}',
      400: '{blue.400}',
      500: '{blue.500}',
      600: '{blue.600}',
      700: '{blue.700}',
      800: '{blue.800}',
      900: '{blue.900}',
      950: '{blue.950}',
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
    providePrimeNG({ theme: { preset: BluePreset, options: { darkModeSelector: '.app-dark' } } }),
    MessageService,
  ],
};
