import { isGranted, PERMISSIONS } from './permissions';

describe('isGranted', () => {
  it('allows when nothing is required', () => {
    expect(isGranted(new Set(), [])).toBe(true);
  });

  it('allows when one of the required permissions is granted', () => {
    expect(isGranted(new Set([PERMISSIONS.USER_READ]), [PERMISSIONS.USER_WRITE, PERMISSIONS.USER_READ])).toBe(true);
  });

  it('allows everything to ADMIN_ALL', () => {
    expect(isGranted(new Set([PERMISSIONS.ADMIN_ALL]), [PERMISSIONS.USER_WRITE])).toBe(true);
  });

  it('denies when none is granted', () => {
    expect(isGranted(new Set([PERMISSIONS.ROLE_READ]), [PERMISSIONS.USER_READ])).toBe(false);
  });
});
