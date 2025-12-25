/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {RandomIndex} from '../random-index';

describe('RandomIndex', () => {
  const array = ['a', 'b', 'c'];

  it('should return an integer', () => {
    expect(Number.isInteger(RandomIndex(array))).toBeTruthy();
  });

  it('should return a index from the provided array', () => {
    expect(array[RandomIndex(array)]).toBeDefined();
  });

  it('should return 0 is the provided array contains only one item', () => {
    expect(RandomIndex(['only item'])).toBe(0);
  });

  it('should throw an error if the provided array is empty', () => {
    expect(() => RandomIndex([])).toThrow();
  });
});
