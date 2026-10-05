import { Routes } from '@angular/router';
import { ChatStore } from './service/chat.store';

export const ASSISTANT_ROUTES: Routes = [
  {
    path: '',
    providers: [ChatStore],
    loadComponent: () => import('./page/assistant-page/assistant-page.component').then((m) => m.AssistantPageComponent),
  },
];
