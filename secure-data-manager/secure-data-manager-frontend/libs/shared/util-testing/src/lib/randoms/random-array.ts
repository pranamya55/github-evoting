/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {RandomInt} from './random-int';

interface Constructable {
}

class Constructor<T extends Constructable> {
  constructor(public readonly construct: new () => T) {
  }
}

export function RandomArray<T extends Constructable>(
    item: new () => T,
    minLength = 1,
): T[] {
  if (minLength < 1)
    throw new Error(
        '[RandomArray] the minimum length should not be greater than 0',
    );
  if (minLength > 10)
    throw new Error(
        '[RandomArray] the minimum length should not be less than or equal to 10',
    );

  const itemConstructor = new Constructor(item);
  return Array.from(
      {length: RandomInt(11, minLength)},
      () => new itemConstructor.construct(),
  );
}
