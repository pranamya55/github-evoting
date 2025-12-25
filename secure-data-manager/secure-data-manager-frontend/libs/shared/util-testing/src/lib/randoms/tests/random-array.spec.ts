/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {RandomArray} from '../random-array';
import {RandomInt} from '../random-int';

jest.mock('../random-int', () => ({
  RandomInt: jest.fn(),
}));

describe('RandomArray', () => {
  class MockClass {}

  it('should return an array containing instances of the provided class', () => {
    RandomArray(MockClass).forEach((randomItem) => {
      expect(randomItem instanceof MockClass).toBeTruthy();
    });
  });

  it('should return a random length array', () => {
    const mockRandomInt = 2;
    (RandomInt as jest.Mock).mockReturnValue(mockRandomInt);

    expect(RandomArray(MockClass).length).toEqual(mockRandomInt);
  });

  it('should return an array with a length less than the minimum length provided', () => {
    const mockMinLength = 3;

    RandomArray(MockClass, mockMinLength);

    expect(RandomInt).toHaveBeenCalledWith(expect.any(Number), mockMinLength);
  });

  it('should throw an error if the provided minimum length is invalid', () => {
    [0, 11].forEach((invalidMinLength) => {
      expect(() => RandomArray(MockClass, invalidMinLength)).toThrow();
    });
  });
});
