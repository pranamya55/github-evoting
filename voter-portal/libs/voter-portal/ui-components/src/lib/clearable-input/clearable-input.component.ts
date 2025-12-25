/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {
	AfterViewInit,
	Component,
	ElementRef,
	Input,
	OnDestroy,
	inject,
} from '@angular/core';

@Component({
	selector: 'vp-clearable-input',
	templateUrl: './clearable-input.component.html',
	standalone: false,
})
export class ClearableInputComponent implements AfterViewInit, OnDestroy {
	private readonly elementRef = inject<ElementRef<HTMLElement>>(ElementRef);

	@Input() buttonLabel = 'common.clear';
	clearableInput: HTMLInputElement | undefined;
	clearButton: HTMLButtonElement | undefined | null;
	isButtonShown = false;
	inputSizeObserver = new ResizeObserver(() => this.adaptButtonToInput());
	inputMutationObserver = new MutationObserver(() => this.adaptButtonToInput());

	ngAfterViewInit() {
		this.clearButton =
			this.elementRef.nativeElement.querySelector<HTMLButtonElement>(
				'.btn-clear',
			);

		const formControls =
			this.elementRef.nativeElement.querySelectorAll<HTMLInputElement>(
				'.form-control',
			);
		if (formControls.length !== 1) {
			throw new Error(
				'The clearable input component must contain exactly one .form-control',
			);
		}

		setTimeout(() => {
			this.clearableInput = formControls[0];
			this.inputSizeObserver.observe(this.clearableInput);
			this.inputMutationObserver.observe(this.clearableInput, {
				attributes: true,
				attributeFilter: ['class'],
			});
			this.clearableInput.addEventListener('change', () =>
				this.adaptButtonToInput(),
			);
			this.clearableInput.addEventListener('input', () =>
				this.adaptButtonToInput(),
			);

			this.updateButtonVisibility();
			this.clearableInput.oninput = () => this.updateButtonVisibility();
		});
	}

	ngOnDestroy() {
		if (this.clearableInput) {
			this.inputSizeObserver.disconnect();
			this.inputMutationObserver.disconnect();
		}
	}

	clearInput() {
		if (this.clearableInput) {
			this.clearableInput.value = '';
			this.clearableInput.dispatchEvent(new Event('input'));
			this.clearableInput.click();
		}
	}

	private updateButtonVisibility(): void {
		if (!this.isButtonShown && !!this.clearableInput?.value) {
			this.isButtonShown = true;
			setTimeout(() => this.adaptButtonToInput());
		}

		if (this.isButtonShown && !this.clearableInput?.value) {
			this.isButtonShown = false;
			setTimeout(() => this.resetInput());
		}
	}

	private resetInput(): void {
		if (this.clearableInput) {
			this.clearableInput.style.paddingRight = '';
		}
	}

	private adaptButtonToInput() {
		if (this.clearableInput && this.clearButton && this.isButtonShown) {
			this.resetInput();

			const {
				borderTopRightRadius: inputBorderTopRightRadius,
				borderBottomRightRadius: inputBorderBottomRightRadius,
				fontSize: inputFontSize,
				paddingLeft: inputPaddingLeft,
				paddingRight: inputPaddingRight,
			} = window.getComputedStyle(this.clearableInput);

			Object.assign(this.clearButton.style, {
				borderTopRightRadius: inputBorderTopRightRadius,
				borderBottomRightRadius: inputBorderBottomRightRadius,
				fontSize: inputFontSize,
				paddingLeft: inputPaddingLeft,
				paddingRight: inputPaddingLeft, // use the left padding as the right one may be enlarged due to validation
				right: `calc(${inputPaddingRight} - ${inputPaddingLeft})`, // places the button before the input validation
			});

			// enlarge the right padding of the input to cover the button and avoid overlap
			const buttonWidth = `${this.clearButton.clientWidth}px`;
			this.clearableInput.style.paddingRight = `calc(${inputPaddingRight} - ${inputPaddingLeft} + ${buttonWidth})`;
		}
	}
}
