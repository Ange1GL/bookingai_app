/** Permission names seeded by the backend; they match its `Authorities` constants. */
export const PERMISSIONS = {
  ADMIN_ALL: 'ADMIN_ALL',
  USER_READ: 'USER_READ',
  USER_WRITE: 'USER_WRITE',
  ROLE_READ: 'ROLE_READ',
} as const;

export type Permission = (typeof PERMISSIONS)[keyof typeof PERMISSIONS];

/** True when `granted` holds ADMIN_ALL or at least one of `required` (an empty `required` means no restriction). */
export function isGranted(granted: ReadonlySet<string>, required: readonly Permission[]): boolean {
  return required.length === 0 || granted.has(PERMISSIONS.ADMIN_ALL) || required.some((permission) => granted.has(permission));
}
