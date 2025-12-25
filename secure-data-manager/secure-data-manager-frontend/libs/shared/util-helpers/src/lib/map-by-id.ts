/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
export function mapById<T extends { id: unknown }>(
  array: T[],
): ReadonlyMap<T['id'], T> {
  const map = new Map();

  array.forEach((item) => {
    map.set(item.id, item);
  });

  return map;
}
