import { Translation } from 'primeng/api';

const MONDAY_FIRST = 1;

/** Spanish labels for PrimeNG widgets (date picker, empty messages). */
export const PRIMENG_ES: Translation = {
  firstDayOfWeek: MONDAY_FIRST,
  dayNames: ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'],
  dayNamesShort: ['dom', 'lun', 'mar', 'mié', 'jue', 'vie', 'sáb'],
  dayNamesMin: ['D', 'L', 'M', 'X', 'J', 'V', 'S'],
  monthNames: ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'],
  monthNamesShort: ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'],
  today: 'Hoy',
  clear: 'Limpiar',
  weekHeader: 'Sem',
  emptyMessage: 'Sin resultados',
  emptyFilterMessage: 'Sin resultados',
};
