/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
let fileIndex = 0;

export class MockFile extends File {
  constructor(extension = '.png') {
    fileIndex++;
    super([], `file-${fileIndex}${extension}`);
  }
}
