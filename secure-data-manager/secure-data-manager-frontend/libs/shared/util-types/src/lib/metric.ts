/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Measurement} from './measurement';

export interface Metric {
  info: string;
  name: string;
  description: string;
  measurements: Measurement[];
  availableTags: [];
}
