/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {BoardMember} from '@sdm/shared-util-types';

let boardMemberIndex = 0;

export class MockBoardMember implements BoardMember {
  id = `boardMember_${boardMemberIndex}`;

  name = `${this.id}_name`;

  constructor() {
    boardMemberIndex++;
  }
}
