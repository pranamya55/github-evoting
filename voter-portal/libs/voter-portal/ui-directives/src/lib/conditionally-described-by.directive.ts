/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Directive, ElementRef, inject, Input, OnDestroy, OnInit,} from '@angular/core';

@Directive({
	// Extends the basic HTML aria-describedby attribute
	// eslint-disable-next-line @angular-eslint/directive-selector
	selector: '[aria-describedby]',
	standalone: false,
})
export class ConditionallyDescribedByDirective implements OnInit, OnDestroy {
	@Input('aria-describedby') ariaDescribedby: string | undefined;
	observer = new MutationObserver(() => this.updateAriaDescribedBy());
	private readonly el = inject<ElementRef<HTMLElement>>(ElementRef);

	get isLabelValid(): boolean {
		return !!this.ariaDescribedby && !!this.ariaDescribedby.trim().length;
	}

	get ariaDescribedbyIds(): string[] {
		const ariaDescribedBy =
			this.el.nativeElement.getAttribute('aria-describedby');
		return ariaDescribedBy ? ariaDescribedBy.split(' ') : [];
	}

	set ariaDescribedbyIds(value: string[]) {
		this.el.nativeElement.setAttribute('aria-describedby', value.join(' '));
	}

	public ngOnInit(): void {
		if (this.isLabelValid) {
			this.observer.observe(document, {subtree: true, childList: true});
		}
	}

	public ngOnDestroy(): void {
		this.observer.disconnect();
	}

	private updateAriaDescribedBy(): void {
		const ids = this.ariaDescribedby?.split(' ') || [];
		ids.forEach((id) => {
			const referenceExists = !!document.querySelector(`#${id}`);
			const idAlreadyRegistered = this.ariaDescribedbyIds.includes(id);

			if (referenceExists && !idAlreadyRegistered) {
				this.ariaDescribedbyIds = [...this.ariaDescribedbyIds, id];
			}

			if (!referenceExists && idAlreadyRegistered) {
				this.ariaDescribedbyIds = this.ariaDescribedbyIds.filter(
					(existingId) => existingId !== id,
				);
			}
		});
	}
}
