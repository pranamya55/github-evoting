/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DynamicHeadingComponent } from './dynamic-heading.component';
import { Component, DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { RandomString } from '@vp/shared-util-testing';

@Component({
	standalone: false,
	template: `
		<vp-dynamic-heading [level]="headingLevel">
			{{ content }}
		</vp-dynamic-heading>
	`,
})
class TestingHost {
	headingLevel = 1;
	content = RandomString();
}

describe('DynamicHeadingComponent', () => {
	const headingLevels = [1, 2, 3, 4, 5, 6];
	let component: TestingHost;
	let fixture: ComponentFixture<TestingHost>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [DynamicHeadingComponent],
			declarations: [TestingHost],
		}).compileComponents();

		fixture = TestBed.createComponent(TestingHost);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	headingLevels.forEach((level) => {
		describe(`heading level: ${level}`, () => {
			let heading: DebugElement;

			beforeEach(() => {
				component.headingLevel = level;
				fixture.detectChanges();
				heading = fixture.debugElement.query(By.css(`h${level}`));
			});

			it('should show the correct heading element', () => {
				expect(heading).toBeTruthy();
			});

			it('should show the content in the heading element', () => {
				expect(heading.nativeElement.textContent).toContain(component.content);
			});

			it('should not show other heading elements', () => {
				headingLevels
					.filter((l) => l !== level)
					.forEach((l) => {
						const otherHeading = fixture.debugElement.query(By.css(`h${l}`));
						expect(otherHeading).toBeFalsy();
					});
			});
		});
	});
});
