/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { NgbModalOptions } from '@ng-bootstrap/ng-bootstrap';
import * as bootstrapIcons from 'bootstrap-icons/font/bootstrap-icons.json';

export interface ConfirmationModalConfig {
	content: string | string[];
	title?: string;
	confirmIcon?: keyof typeof bootstrapIcons;
	confirmLabel?: string;
	cancelLabel?: string;
	modalOptions?: NgbModalOptions;
}
