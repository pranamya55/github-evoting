/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, EventEmitter, Input, OnChanges, Output, SimpleChanges,} from '@angular/core';
import {FormControl} from '@angular/forms';
import {BoardMember} from '@sdm/shared-util-types';

@Component({
	selector: 'sdm-password-abstract',
	standalone: true,
	template: '',
})
export abstract class PasswordAbstractComponent implements OnChanges {
	@Input({required: true}) boardMembers!: BoardMember[];
	@Output() allPasswordsSet = new EventEmitter<string[]>();

	activeMember: BoardMember | undefined;
	passwords = new Map<BoardMember['id'], string>();
	abstract password: FormControl<string>;

	ngOnChanges(changes: SimpleChanges) {
		if (changes['boardMembers']) {
			this.updateActiveMember();
		}
	}

	protected registerPassword() {
		if (!this.activeMember) return;

		this.passwords.set(this.activeMember.id, this.password.value);
		this.updateActiveMember();
	}

	public updateActiveMember() {
		const memberWithoutPassword = this.boardMembers.find(
			(member) => !this.passwords.has(member.id),
		);

		if (!memberWithoutPassword) {
			this.allPasswordsSet.emit(Array.from(this.passwords.values()));
		}

		this.activeMember = memberWithoutPassword;
	}
}
