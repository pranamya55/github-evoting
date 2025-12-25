/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {RandomInt} from './random-int';

export function RandomItem<T>(
  iterable: T[] | Record<string, T>,
  filter?: (value: T, index: number) => boolean,
): T {
  let arr = Object.values(iterable);

  if (!arr.length)
    throw new Error(
      `[RandomItem] the ${
        Array.isArray(iterable) ? 'array' : 'object'
      } provided is empty`,
    );

  if (filter) {
    arr = arr.filter(filter);
  }

  if (!arr.length)
    throw new Error(
      `[RandomItem] the ${
        Array.isArray(iterable) ? 'array' : 'object'
      } provided is empty after applying the filter`,
    );

  return arr[RandomInt(arr.length)];
}
