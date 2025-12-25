/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {AfterViewInit, Component, DestroyRef, HostBinding, inject, Input} from '@angular/core';

import {TranslateModule} from '@ngx-translate/core';
import {distinctUntilChanged, fromEvent, map} from "rxjs";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";

@Component({
	selector: 'sdm-page-title',
	standalone: true,
	imports: [TranslateModule],
	templateUrl: './page-title.component.html',
})
export class PageTitleComponent implements AfterViewInit {
	@Input({required: true}) title!: string;
	@Input({required: true}) instructions!: string;

	@HostBinding('class.sticky') isSticky = false;

	destroyRef = inject(DestroyRef);

	ngAfterViewInit() {
		const main = document.querySelector('main');

		if (!main) return;

		fromEvent(main, 'scroll')
			.pipe(
				map(() => main.scrollTop !== 0),
				distinctUntilChanged(),
				takeUntilDestroyed(this.destroyRef)
			)
			.subscribe(isParentScrolled => {
				this.isSticky = isParentScrolled;
			});
	}
}
