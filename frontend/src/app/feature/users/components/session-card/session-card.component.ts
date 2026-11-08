import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { IconButtonComponent } from '@/shared/components/icon-button/icon-button.component';
import { DeviceKind, parseUserAgent } from '@/shared/utils/parse-user-agent';
import { UserSessionDto } from '../../models/user-session.dto';

const DEVICE_ICONS: Record<DeviceKind, string> = {
  desktop: 'pi-desktop',
  mobile: 'pi-mobile',
  tablet: 'pi-tablet',
  unknown: 'pi-question-circle',
};
const LOCALE = 'es-MX';
const MINUTE_MS = 60_000;
const HOUR_MS = 60 * MINUTE_MS;
const DAY_MS = 24 * HOUR_MS;
const JUST_NOW_LABEL = 'hace un momento';

const dateTimeFormat = new Intl.DateTimeFormat(LOCALE, { dateStyle: 'medium', timeStyle: 'short' });
const relativeFormat = new Intl.RelativeTimeFormat(LOCALE, { numeric: 'auto' });

function formatDateTime(iso: string): string {
  return dateTimeFormat.format(new Date(iso));
}

/** "hace 5 minutos", "hace 2 horas", "hace 3 días"; anything under a minute reads as "hace un momento". */
function formatRelative(iso: string, now: number): string {
  const elapsed = now - new Date(iso).getTime();
  if (elapsed < MINUTE_MS) {
    return JUST_NOW_LABEL;
  }
  if (elapsed < HOUR_MS) {
    return relativeFormat.format(-Math.floor(elapsed / MINUTE_MS), 'minute');
  }
  if (elapsed < DAY_MS) {
    return relativeFormat.format(-Math.floor(elapsed / HOUR_MS), 'hour');
  }
  return relativeFormat.format(-Math.floor(elapsed / DAY_MS), 'day');
}

/** One active session: device, IP and dates, with a button to close it. */
@Component({
  selector: 'app-session-card',
  imports: [IconButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './session-card.component.html',
})
export class SessionCardComponent {
  readonly session = input.required<UserSessionDto>();
  readonly busy = input(false);
  readonly closeRequested = output<UserSessionDto>();

  private readonly device = computed(() => parseUserAgent(this.session().userAgent));

  protected readonly icon = computed(() => DEVICE_ICONS[this.device().device]);
  protected readonly title = computed(() => `${this.device().browser} en ${this.device().os}`);
  protected readonly ipAddress = computed(() => this.session().ipAddress ?? 'IP desconocida');
  protected readonly startedAt = computed(() => formatDateTime(this.session().startedAt));
  protected readonly lastActivity = computed(() => formatRelative(this.session().lastActivityAt, Date.now()));
  protected readonly expiresAt = computed(() => formatDateTime(this.session().expiresAt));
  protected readonly closeTooltip = computed(() => `Cerrar la sesión de ${this.title()}`);
}
