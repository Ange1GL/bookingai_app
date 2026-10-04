import { DisplayStatus } from './calendar.model';

export interface StatusStyle {
  label: string;
  icon: string;
  /** Dot / accent bar colour. */
  dot: string;
  /** Pill used for the status chip. */
  chip: string;
  /** Compact event chip inside month cells (desktop). */
  event: string;
}

export const STATUS_STYLE: Record<DisplayStatus, StatusStyle> = {
  reserved: {
    label: 'Reservada',
    icon: 'pi-calendar',
    dot: 'bg-primary-500',
    chip: 'bg-primary-100 text-primary-700 dark:bg-primary-500/20 dark:text-primary-300',
    event: 'bg-primary-100 text-primary-800 dark:bg-primary-500/20 dark:text-primary-200',
  },
  'in-progress': {
    label: 'En curso',
    icon: 'pi-clock',
    dot: 'bg-emerald-500',
    chip: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/20 dark:text-emerald-300',
    event: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-500/20 dark:text-emerald-200',
  },
  finished: {
    label: 'Finalizada',
    icon: 'pi-check-circle',
    dot: 'bg-slate-400',
    chip: 'bg-slate-100 text-slate-600 dark:bg-slate-500/20 dark:text-slate-300',
    event: 'bg-slate-100 text-slate-600 dark:bg-slate-500/20 dark:text-slate-300',
  },
  cancelled: {
    label: 'Cancelada',
    icon: 'pi-times-circle',
    dot: 'bg-rose-400',
    chip: 'bg-rose-100 text-rose-700 dark:bg-rose-500/20 dark:text-rose-300',
    event: 'bg-rose-100 text-rose-700 line-through dark:bg-rose-500/20 dark:text-rose-300',
  },
};
