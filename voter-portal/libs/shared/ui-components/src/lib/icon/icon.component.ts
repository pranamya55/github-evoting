/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, HostBinding, Input } from '@angular/core';
import * as bootstrapIcons from 'bootstrap-icons/font/bootstrap-icons.json';
import votingCardIcons from './voting-card-icons';
import {NgClass} from "@angular/common";

export type VotingCardIconName = keyof typeof votingCardIcons;
export type BootstrapIconName = keyof typeof bootstrapIcons;

@Component({
	selector: 'vp-icon',
	templateUrl: './icon.component.html',
	standalone: true,
	imports: [
		NgClass
	]
})
export class IconComponent {
	@Input() name!: BootstrapIconName | VotingCardIconName;

	@HostBinding('attr.aria-label') @Input() label: string | undefined;

	@HostBinding('attr.role') role = 'img';

	@HostBinding('attr.aria-hidden') get hidden() {
		return String(!this.label);
	}

	get isBootstrapIcon(): boolean {
		return Object.keys(bootstrapIcons).includes(this.name);
	}

	get isVotingCardIcon(): boolean {
		return Object.keys(votingCardIcons).includes(this.name);
	}

	get votingCardIconPath(): string {
		return votingCardIcons[this.name as VotingCardIconName];
	}
}
