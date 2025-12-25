/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {RandomItem} from '../random-item';
import {RandomBoolean} from '../random-boolean';

jest.mock('../random-item', () => ({
  RandomItem: jest.fn(),
}));

describe('RandomBoolean', () => {
  it('should call RandomItem with the two boolean', () => {
    RandomBoolean();

    expect(RandomItem).toHaveBeenNthCalledWith(1, [true, false]);
  });

  it('should return the value returned by RandomItem', () => {
    const expectedReturnValue = 'return value';

    (RandomItem as jest.Mock).mockReturnValue(expectedReturnValue);

    expect(RandomBoolean()).toBe(expectedReturnValue);
  });
});
