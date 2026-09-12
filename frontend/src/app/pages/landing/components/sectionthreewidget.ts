import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { LazyImageWidget } from '@/pages/landing/components/lazyimagewidget';
import { LayoutService } from '@/layout/service/layout.service';

@Component({
    selector: 'app-section-three-widget',
    standalone: true,
    imports: [CommonModule, LazyImageWidget],
    template: `
        <section class="relative py-12 lg:py-24">
            <div class="landing-container mx-auto w-full flex flex-col items-center">
                <div class="badge mx-0">Features</div>
                <h4 class="title-h4 lg:title-h2 text-center mt-6">Version Control <span class="text-primary-600">Systems</span></h4>
                <p class="body-small md:body-large text-center mt-6 max-w-4xl">
                    Justo eget magna fermentum iaculis eu non diam phasellus vestibulum. Sed tempus urna et pharetra pharetra massa massa ultricies. Ut faucibus pulvinar elementum integer enim.
                </p>
            </div>
            <div class="scale-50 md:scale-75 lg:scale-100 w-full h-200 md:h-272 lg:h-auto -mt-48 md:-mt-32 lg:mt-0">
                <div class="w-full h-[71.6rem] relative mt-1">
                    <div class="absolute scale-100 top-0 left-1/2 -translate-x-1/2 h-full w-408 mx-auto flex items-end justify-end overflow-hidden">
                        <div
                            class="z-10 absolute top-0 w-full h-64 lg:h-72 [background:linear-gradient(0deg,rgba(255,255,255,0.00)_0%,var(--p-surface-0)_84.42%,var(--p-surface-0)_100%)] dark:[background:linear-gradient(0deg,rgba(255,255,255,0.00)_0%,var(--p-surface-950)_84.42%,var(--p-surface-950)_100%)] transition-all"
                        ></div>
                        <div
                            class="z-10 absolute bottom-0 w-full h-128 lg:h-144 [background:linear-gradient(180deg,rgba(255,255,255,0.00)_0%,var(--p-surface-0)_39.73%,var(--p-surface-0)_100%)] dark:[background:linear-gradient(180deg,rgba(255,255,255,0.00)_0%,var(--p-surface-950)_39.73%,var(--p-surface-950)_100%)] transition-all"
                        ></div>
                        <div class="z-2 absolute top-40 left-60 animate-float">
                            <app-lazy-image-widget
                                className="rounded-3xl w-100 rotate-[-16deg] shadow-[0px_112px_31px_0px_rgba(0,0,0,0.00),0px_72px_29px_0px_rgba(0,0,0,0.01),0px_40px_24px_0px_rgba(0,0,0,0.03),0px_18px_18px_0px_rgba(0,0,0,0.05),0px_4px_10px_0px_rgba(0,0,0,0.06),0px_0.697px_1.394px_0px_rgba(18,18,23,0.05),0px_0.569px_1.139px_0px_rgba(18,18,23,0.05),0px_1px_2px_0px_rgba(18,18,23,0.05)]"
                                [src]="'/demo/images/landing/boxes/' + (isDarkTheme() ? 'credit-score-dark.png' : 'credit-score.png')"
                                alt="Features Section One Box Image"
                            />
                        </div>
                        <div class="z-3 absolute top-28 left-128">
                            <app-lazy-image-widget
                                className="rounded-2xl w-100 rotate-[-8deg] shadow-[0px_112px_31px_0px_rgba(0,0,0,0.00),0px_72px_29px_0px_rgba(0,0,0,0.01),0px_40px_24px_0px_rgba(0,0,0,0.03),0px_18px_18px_0px_rgba(0,0,0,0.05),0px_4px_10px_0px_rgba(0,0,0,0.06),0px_0.697px_1.394px_0px_rgba(18,18,23,0.05),0px_0.569px_1.139px_0px_rgba(18,18,23,0.05)]"
                                [src]="'/demo/images/landing/boxes/' + (isDarkTheme() ? 'sales-rate-dark.png' : 'sales-rate.png')"
                                alt="Features Section One Box Image"
                            />
                        </div>
                        <div class="z-4 absolute top-34 left-166 animate-float animate-delay-1000">
                            <app-lazy-image-widget
                                className="rounded-3xl w-100 rotate-[5deg] shadow-[0px_112px_31px_0px_rgba(0,0,0,0.00),0px_72px_29px_0px_rgba(0,0,0,0.01),0px_40px_24px_0px_rgba(0,0,0,0.03),0px_18px_18px_0px_rgba(0,0,0,0.05),0px_4px_10px_0px_rgba(0,0,0,0.06),0px_0.697px_1.394px_0px_rgba(18,18,23,0.05),0px_0.569px_1.139px_0px_rgba(18,18,23,0.05)]"
                                [src]="'/demo/images/landing/boxes/' + (isDarkTheme() ? 'social-media-users-dark.png' : 'social-media-users.png')"
                                alt="Features Section One Box Image"
                            />
                        </div>
                        <div class="z-3 absolute top-[10.3rem] left-264 animate-float animate-delay-300">
                            <app-lazy-image-widget
                                className="rounded-2xl w-100 rotate-22 shadow-[0px_112px_31px_0px_rgba(0,0,0,0.00),0px_72px_29px_0px_rgba(0,0,0,0.01),0px_40px_24px_0px_rgba(0,0,0,0.03),0px_18px_18px_0px_rgba(0,0,0,0.05),0px_4px_10px_0px_rgba(0,0,0,0.06),0px_0.697px_1.394px_0px_rgba(18,18,23,0.05),0px_0.569px_1.139px_0px_rgba(18,18,23,0.05)]"
                                [src]="'/demo/images/landing/boxes/' + (isDarkTheme() ? 'new-customer-dark.png' : 'new-customer.png')"
                                alt="Features Section One Box Image"
                            />
                        </div>
                        <div class="z-4 absolute top-120 left-284 animate-float animate-delay-300 animate-duration-[8s]">
                            <app-lazy-image-widget
                                className="rounded-3xl w-100 rotate-[-8.5deg] shadow-[0px_112px_31px_0px_rgba(0,0,0,0.00),0px_72px_29px_0px_rgba(0,0,0,0.01),0px_40px_24px_0px_rgba(0,0,0,0.03),0px_18px_18px_0px_rgba(0,0,0,0.05),0px_4px_10px_0px_rgba(0,0,0,0.06),0px_0.697px_1.394px_0px_rgba(18,18,23,0.05),0px_0.569px_1.139px_0px_rgba(18,18,23,0.05),0px_1px_2px_0px_rgba(18,18,23,0.05)]"
                                [src]="'/demo/images/landing/boxes/' + (isDarkTheme() ? 'spending-limit-dark.png' : 'spending-limit.png')"
                                alt="Features Section One Box Image"
                            />
                        </div>
                        <div class="z-1 absolute top-98 left-[3.8rem] animate-wiggle">
                            <app-lazy-image-widget
                                className="rounded-2xl w-74 shadow-[0px_112px_31px_0px_rgba(0,0,0,0.00),0px_72px_29px_0px_rgba(0,0,0,0.01),0px_40px_24px_0px_rgba(0,0,0,0.03),0px_18px_18px_0px_rgba(0,0,0,0.05),0px_4px_10px_0px_rgba(0,0,0,0.06),0px_0.697px_1.394px_0px_rgba(18,18,23,0.05),0px_0.569px_1.139px_0px_rgba(18,18,23,0.05)]"
                                [src]="'/demo/images/landing/boxes/' + (isDarkTheme() ? 'try-chart-dark.png' : 'try-chart.png')"
                                alt="Features Section One Box Image"
                            />
                        </div>
                        <div class="z-3 absolute top-122 left-118">
                            <app-lazy-image-widget
                                className="rounded-3xl w-74 rotate-[2.5deg] shadow-[0px_112px_31px_0px_rgba(0,0,0,0.00),0px_72px_29px_0px_rgba(0,0,0,0.01),0px_40px_24px_0px_rgba(0,0,0,0.03),0px_18px_18px_0px_rgba(0,0,0,0.05),0px_4px_10px_0px_rgba(0,0,0,0.06),0px_0.697px_1.394px_0px_rgba(18,18,23,0.05),0px_0.569px_1.139px_0px_rgba(18,18,23,0.05),0px_1px_2px_0px_rgba(18,18,23,0.05)]"
                                [src]="'/demo/images/landing/boxes/' + (isDarkTheme() ? 'eur-chart-dark.png' : 'eur-chart.png')"
                                alt="Features Section One Box Image"
                            />
                        </div>
                        <div class="z-5 absolute top-[41.3rem] left-[51.3rem]">
                            <app-lazy-image-widget
                                className="rounded-2xl w-74 rotate-[-14.5deg] shadow-[0px_1px_2px_0px_rgba(18,18,23,0.05)]"
                                [src]="'/demo/images/landing/boxes/' + (isDarkTheme() ? 'usd-chart-dark.png' : 'usd-chart.png')"
                                alt="Features Section One Box Image"
                            />
                        </div>
                        <div class="z-1 absolute top-[28.8rem] left-260 animate-wiggle animate-delay-[5s]">
                            <app-lazy-image-widget
                                className="rounded-3xl w-74 rotate-22 shadow-[0px_112px_31px_0px_rgba(0,0,0,0.00),0px_72px_29px_0px_rgba(0,0,0,0.01),0px_40px_24px_0px_rgba(0,0,0,0.03),0px_18px_18px_0px_rgba(0,0,0,0.05),0px_4px_10px_0px_rgba(0,0,0,0.06),0px_0.697px_1.394px_0px_rgba(18,18,23,0.05),0px_0.569px_1.139px_0px_rgba(18,18,23,0.05),0px_1px_2px_0px_rgba(18,18,23,0.05)]"
                                [src]="'/demo/images/landing/boxes/' + (isDarkTheme() ? 'gbp-chart-dark.png' : 'gbp-chart.png')"
                                alt="Features Section One Box Image"
                            />
                        </div>
                        <div class="z-3 absolute top-[36.6rem] left-4 animate-float animate-duration-[5s] animate-delay-[4s]">
                            <app-lazy-image-widget
                                className="rounded-3xl w-208 rotate-[9.3deg] shadow-[0px_112px_31px_0px_rgba(0,0,0,0.00),0px_72px_29px_0px_rgba(0,0,0,0.01),0px_-40px_24px_0px_rgba(0,0,0,0.02),0px_-4px_18px_0px_rgba(0,0,0,0.03),0px_-4px_10px_0px_rgba(0,0,0,0.04),0px_0.697px_1.394px_0px_rgba(18,18,23,0.03),0px_0.569px_1.139px_0px_rgba(18,18,23,0.03),0px_-1px_2px_0px_rgba(18,18,23,0.03)]"
                                [src]="'/demo/images/landing/boxes/' + (isDarkTheme() ? 'income-dark.png' : 'income.png')"
                                alt="Features Section One Box Image"
                            />
                        </div>
                    </div>
                </div>
            </div>
            <div class="-mt-32 md:-mt-56 lg:-mt-64 max-w-md px-6 md:landing-container w-full mx-auto relative z-50 grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4 lg:gap-7">
                @for (data of details; track data; let index = $index) {
                    <div class="p-2.5 lg:p-3.5 flex items-start gap-3.5">
                        <div class="bg-primary w-12 h-10" [ngStyle]="{ mask: 'url(' + data.icon + ') no-repeat center' }"></div>
                        <div class="flex-1">
                            <span class="title-h7">{{ data.title }}</span>
                            <p class="body-small mt-2 text-left text-surface-700 dark:text-surface-400">{{ data.description }}</p>
                        </div>
                    </div>
                }
            </div>
        </section>
    `
})
export class SectionThreeWidget {
    layoutService = inject(LayoutService);

    isDarkTheme = computed(() => this.layoutService.isDarkTheme());

    details = [
        {
            title: 'Change Tracking',
            description: 'Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.',
            icon: '/demo/images/landing/icons/icon-1.svg'
        },
        {
            title: 'Branching and Merging',
            description: 'Ornare suspendisse sed nisi lacus sed viverra tellus. Neque volutpat ac tincidunt vitae semper.',
            icon: '/demo/images/landing/icons/icon-2.svg'
        },
        {
            title: 'Collaboration',
            description: 'Risus nec feugiat in fermentum posuere urna nec. Posuere sollicitudin aliquam ultrices sagittis.',
            icon: '/demo/images/landing/icons/icon-3.svg'
        },
        {
            title: 'Conflict Resolution',
            description: 'Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.',
            icon: '/demo/images/landing/icons/icon-4.svg'
        },
        {
            title: 'Revert to Previous Versions',
            description: 'Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.',
            icon: '/demo/images/landing/icons/icon-5.svg'
        },
        {
            title: 'Backup and Disaster Recovery',
            description: 'Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.',
            icon: '/demo/images/landing/icons/icon-6.svg'
        }
    ];
}
