/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Locale } from './locale';
import {TranslatableText} from "e-voting-libraries-ui-kit";


export interface Environment {
  production: boolean;
  workflowEnabled: boolean;
  backendPath: string;
  remoteServerAvailable: boolean;
  locales: Locale[];
  defaultLang: Lowercase<keyof TranslatableText>;
}
