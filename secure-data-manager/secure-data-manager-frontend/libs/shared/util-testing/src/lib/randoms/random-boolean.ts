/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {RandomItem} from './random-item';

export function RandomBoolean(): boolean {
  return RandomItem([true, false]);
}
