/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ConfigurationFile} from '@sdm/shared-util-types';

let configIndex = 0;

export class MockElectionEventConfig implements ConfigurationFile {
  alias = `electionEvent_${configIndex}`;
  date = new Date().toDateString();
  descriptions = {
    de: `${this.alias}_defaultTitle_DE`,
    fr: `${this.alias}_defaultTitle_FR`,
    it: `${this.alias}_defaultTitle_IT`,
  };
  files = [];

  constructor() {
    configIndex++;
  }
}
