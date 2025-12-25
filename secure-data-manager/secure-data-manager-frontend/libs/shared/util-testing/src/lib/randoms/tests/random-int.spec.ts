/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {RandomInt} from '../random-int';

describe('RandomInt', () => {
  it('should return a random integer', () => {
    expect(Number.isInteger(RandomInt())).toBeTruthy();
  });

  it('should return a integer less than the maximum provided', () => {
    expect(RandomInt(1)).toEqual(0);
  });

  it('should return a integer greater than or equal to the minimum provided', () => {
    expect(RandomInt(2, 1)).toEqual(1);
  });

  it('should throw an error if the provided minimum is greater than or equal to provided maximum', () => {
    expect(() => RandomInt(2, 2)).toThrow();
  });
});
