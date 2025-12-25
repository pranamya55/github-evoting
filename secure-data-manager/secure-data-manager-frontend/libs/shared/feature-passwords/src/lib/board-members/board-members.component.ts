/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */


import {Component, Input, OnChanges, OnInit} from '@angular/core';
import {TranslateModule} from '@ngx-translate/core';
import {BoardMember} from '@sdm/shared-util-types';
import {ReactiveFormsModule} from '@angular/forms';

@Component({
	selector: 'sdm-board-members',
	standalone: true,
	imports: [
		TranslateModule,
		ReactiveFormsModule
	],
	templateUrl: './board-members.component.html',
})
export class BoardMembersComponent implements OnInit, OnChanges {
	@Input({required: true}) boardMembers!: BoardMember[];
	@Input({required: true}) activeMember?: BoardMember;
	@Input({required: true}) passwords!: ReadonlyMap<BoardMember['id'], string>;

	membersWithPassword!: BoardMember[];

	showActiveMember = false;

	ngOnInit() {
		// so that the active board member is announced by screen readers after page load
		setTimeout(() => {
			this.showActiveMember = true;
		}, 500);
	}

	ngOnChanges() {
		this.membersWithPassword = [];

		this.boardMembers.forEach((member) => {
			if (this.passwords.has(member.id)) {
				this.membersWithPassword.push(member);
			}
		});
	}
}
