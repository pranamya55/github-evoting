/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, Input } from '@angular/core';
import { ElectionTexts } from 'e-voting-libraries-ui-kit';

@Component({
	selector: 'vp-verify-election-group',
	templateUrl: './verify-election-group.component.html',
	standalone: false,
})
export class VerifyElectionGroupComponent {
	@Input({ required: true }) electionTexts!: ElectionTexts;
}
