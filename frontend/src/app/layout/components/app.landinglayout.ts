import { Component } from '@angular/core';

import { RouterModule } from '@angular/router';
import { FooterWidget } from '@/pages/landing/components/footerwidget';
import { TopbarWidget } from '@/pages/landing/components/topbarwidget';

@Component({
    selector: 'app-landing-layout',
    standalone: true,
    imports: [TopbarWidget, RouterModule, FooterWidget],
    template: ` <app-topbar-widget />
        <main>
            <router-outlet></router-outlet>
        </main>
        <app-footer-widget />`
})
export class LandingLayout {}
