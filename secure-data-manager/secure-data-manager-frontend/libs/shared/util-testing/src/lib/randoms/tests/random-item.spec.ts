/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {RandomInt} from '../random-int';
import {RandomItem} from '../random-item';

jest.mock('../random-int', () => ({
  RandomInt: jest.fn(),
}));

describe('RandomItem', () => {
  describe('on array', () => {
    const mockArray = [1, 'a', false, 'B', 42];

    it('should return an item from the provided array', () => {
      const mockRandomIndex = 2;

      (RandomInt as jest.Mock).mockReturnValue(mockRandomIndex);

      expect(RandomItem(mockArray)).toBe(mockArray[mockRandomIndex]);
      expect(RandomInt).toHaveBeenCalledWith(mockArray.length);
    });

    it('should not return a filtered item from the array provided', () => {
      RandomItem(mockArray, (value) => value === 42);
      expect(RandomInt).toHaveBeenCalledWith(1);
    });

    it('should throw an error if the provided array is empty', () => {
      expect(() => RandomItem([])).toThrow();
    });

    it('should throw an error if the provided array is empty after applying the filter', () => {
      const filterAll = () => false;
      expect(() => RandomItem(mockArray, filterAll)).toThrow();
    });
  });

  describe('on object', () => {
    const mockObject = {
      a: 'somestring',
      b: 42,
      c: false,
    };

    it('should return an value from the provided object', () => {
      const mockRandomKey = 'c';

      (RandomInt as jest.Mock).mockReturnValue(
        Object.keys(mockObject).indexOf(mockRandomKey),
      );

      expect(RandomItem(mockObject)).toBe(mockObject[mockRandomKey]);
      expect(RandomInt).toHaveBeenCalledWith(Object.values(mockObject).length);
    });

    it('should not return a filtered item from the object provided', () => {
      RandomItem(mockObject, (value) => value === 'somestring');
      expect(RandomInt).toHaveBeenCalledWith(1);
    });

    it('should throw an error if the provided object is empty', () => {
      expect(() => RandomItem({})).toThrow();
    });

    it('should throw an error if the provided object is empty after applying the filter', () => {
      const filterAll = () => false;
      expect(() => RandomItem(mockObject, filterAll)).toThrow();
    });
  });
});
