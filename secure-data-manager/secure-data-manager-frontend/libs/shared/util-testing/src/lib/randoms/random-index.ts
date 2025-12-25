/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {RandomInt} from './random-int';

export function RandomIndex<T>(arr: T[]): number {
  if (!arr.length) throw new Error(`[RandomIndex] the array provided is empty`);
  return RandomInt(arr.length);
}
