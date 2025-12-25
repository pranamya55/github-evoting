/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, Input } from '@angular/core';
import { ElectionTexts } from 'e-voting-libraries-ui-kit';

@Component({
	selector: 'vp-review-election-group',
	templateUrl: './review-election-group.component.html',
	standalone: false,
})
export class ReviewElectionGroupComponent {
	@Input({ required: true }) electionTexts!: ElectionTexts;
}
