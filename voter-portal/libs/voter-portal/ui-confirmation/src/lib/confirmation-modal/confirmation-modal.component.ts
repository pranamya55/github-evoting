/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, Input, OnInit, inject } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { BootstrapIconName } from "@vp/shared-ui-components";

function checkContents(contents: string | string[]) {
	if (!contents || !contents.length) {
		throw new Error('The ConfirmationModalComponent requires contents.');
	}
}

@Component({
	selector: 'vp-confirmation-modal',
	templateUrl: './confirmation-modal.component.html',
	standalone: false,
})
export class ConfirmationModalComponent implements OnInit {
	readonly activeModal = inject(NgbActiveModal);

	@Input() title!: string;
	@Input() confirmIcon?: BootstrapIconName;
	@Input() confirmLabel!: string;
	@Input() cancelLabel!: string;

	private _content: string[] = [];

	get content(): string[] {
		return this._content;
	}

	@Input() set content(value: string | string[]) {
		checkContents(value);
		this._content = Array.isArray(value) ? value : [value];
	}

	ngOnInit() {
		checkContents(this._content);
	}
}
