import { buildPageItems, PAGE_GAP } from './pagination.util';

describe('buildPageItems', () => {
  it('lists every page when they all fit', () => {
    expect(buildPageItems(0, 1)).toEqual([0]);
    expect(buildPageItems(3, 7)).toEqual([0, 1, 2, 3, 4, 5, 6]);
  });

  it('collapses the far end near the start', () => {
    expect(buildPageItems(0, 20)).toEqual([0, 1, 2, 3, 4, PAGE_GAP, 19]);
  });

  it('collapses both sides in the middle', () => {
    expect(buildPageItems(10, 20)).toEqual([0, PAGE_GAP, 9, 10, 11, PAGE_GAP, 19]);
  });

  it('collapses the start near the end', () => {
    expect(buildPageItems(19, 20)).toEqual([0, PAGE_GAP, 15, 16, 17, 18, 19]);
  });
});
