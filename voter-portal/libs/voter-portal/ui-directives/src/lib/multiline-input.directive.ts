/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {
	AfterViewInit,
	Directive,
	ElementRef,
	HostListener,
	OnDestroy,
	OnInit,
	inject,
} from '@angular/core';

@Directive({
	selector: 'textarea[vpMultilineInput]',
	standalone: false,
})
export class MultilineInputDirective
	implements OnInit, AfterViewInit, OnDestroy
{
	private readonly el = inject<ElementRef<HTMLTextAreaElement>>(ElementRef);

	skipNextResize = false;

	observer = new ResizeObserver(() => {
		if (this.skipNextResize) {
			this.skipNextResize = false;
			return;
		}

		this.updateInputHeight();
	});

	constructor() {
		this.el.nativeElement.style.height = '0';
		this.el.nativeElement.style.resize = 'none';
		this.el.nativeElement.style.overflow = 'hidden';
	}

	@HostListener('input', ['$event.target.value'])
	public onInput(value: string): void {
		// Avoid the ResizeObserver callback being called when a first character is entered or a last character is deleted
		if (value.length < 2) this.skipNextResize = true;
	}

	public ngOnInit(): void {
		this.observer.observe(this.el.nativeElement);
	}

	public ngAfterViewInit(): void {
		this.updateInputHeight();
	}

	public ngOnDestroy(): void {
		this.observer.disconnect();
	}

	updateInputHeight(): void {
		const { value, placeholder } = this.el.nativeElement;

		// if there is no value, use the placeholder
		if (value === '' && placeholder) {
			this.el.nativeElement.value = placeholder;
		}

		this.el.nativeElement.style.height = `${this.el.nativeElement.scrollHeight}px`;

		// reset initial value
		if (value === '' && placeholder) {
			this.el.nativeElement.value = value;
		}
	}
}
