/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {
	AfterContentInit,
	Component,
	ElementRef,
	HostBinding,
	HostListener,
	inject,
	Input,
} from '@angular/core';

@Component({
	selector: 'vp-answer',
	standalone: false,
	template: ` <ng-content /> `,
})
export class AnswerComponent implements AfterContentInit {
	@HostBinding('class.active') @Input() isActive?: boolean;

	formControl: HTMLInputElement | null = null;
	private readonly el = inject(ElementRef);

	ngAfterContentInit() {
		this.formControl = this.el.nativeElement.querySelector('input');
	}

	@HostListener('click', ['$event'])
	handleClick(e: MouseEvent) {
		if (!this.formControl) return;

		const target = e.target as Element;
		const isFormControl = target.isEqualNode(this.formControl);
		const isFormControlLabel =
			target.localName === 'label' &&
			target.getAttribute('for') === this.formControl.id;

		this.formControl.focus();

		if (!isFormControl && !isFormControlLabel) {
			this.formControl.click();
		}
	}
}
