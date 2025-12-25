/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RandomKey } from '@vp/shared-util-testing';
import * as bootstrapIcons from 'bootstrap-icons/font/bootstrap-icons.json';
import { IconComponent } from './icon.component';
import votingCardIcons from './voting-card-icons';

describe('IconComponent', () => {
	let hostElement: HTMLElement;
	let component: IconComponent;
	let fixture: ComponentFixture<IconComponent>;

	const setComponentLabel = (label: string): void => {
		component.label = label;
		fixture.detectChanges();
	};

	const setComponentName = (name: string): void => {
		component.name = name as typeof component.name;
		fixture.detectChanges();
	};

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [IconComponent],
		}).compileComponents();
	});

	beforeEach(() => {
		fixture = TestBed.createComponent(IconComponent);
		component = fixture.componentInstance;
		hostElement = fixture.nativeElement;
		fixture.detectChanges();
	});

	it('should have a role="img" attribute', () => {
		expect(hostElement.getAttribute('role')).toBe('img');
	});

	it('should be aria-hidden if no label is set', () => {
		expect(hostElement.getAttribute('aria-hidden')).toBe('true');
	});

	it('should not be aria-hidden if a label is set', () => {
		setComponentLabel('Testing label.');

		expect(hostElement.getAttribute('aria-hidden')).toBe('false');
	});

	it('should not have an aria-label if no label is set', () => {
		expect(hostElement.getAttribute('aria-label')).toBeNull();
	});

	it('should not be aria-hidden if a label is set', () => {
		const testingLabel = 'Another testing label.';
		setComponentLabel(testingLabel);

		expect(hostElement.getAttribute('aria-label')).toBe(testingLabel);
	});

	describe('bootstrap icons', () => {
		let bootstrapIconName: string;
		let bootstrapIconElement: HTMLElement;

		beforeEach(() => {
			bootstrapIconName = RandomKey(bootstrapIcons);
			setComponentName(bootstrapIconName);
			bootstrapIconElement = hostElement.children.item(0) as HTMLElement;
		});

		it('should show exactly one "i" element', () => {
			expect(hostElement.children.length).toBe(1);
			expect(bootstrapIconElement.tagName).toBe('I');
		});

		it('should be aria-hidden', () => {
			expect(bootstrapIconElement.getAttribute('aria-hidden')).toBe('true');
		});

		it('should have the desired bootstrap icon class', () => {
			expect(bootstrapIconElement.classList).toContain(
				'bi-' + bootstrapIconName,
			);
		});
	});

	describe('voting card icons', () => {
		let votingCardIconName: string;
		let votingCardIconElement: HTMLElement;
		let votingCardIconPath: HTMLElement;

		beforeEach(() => {
			votingCardIconName = RandomKey(votingCardIcons);
			setComponentName(votingCardIconName);
			votingCardIconElement = hostElement.children.item(0) as HTMLElement;
			votingCardIconPath = votingCardIconElement.children.item(
				0,
			) as HTMLElement;
		});

		it('should show exactly one "svg" element', () => {
			expect(hostElement.children.length).toBe(1);
			expect(votingCardIconElement.tagName).toBe('svg');
		});

		it('should be aria-hidden', () => {
			expect(votingCardIconElement.getAttribute('aria-hidden')).toBe('true');
		});

		it('should show exactly one "path" element', () => {
			expect(votingCardIconElement.children.length).toBe(1);
			expect(votingCardIconPath.tagName).toBe('path');
		});

		it('should have the desired path definition', () => {
			const expectedPath =
				votingCardIcons[votingCardIconName as keyof typeof votingCardIcons];
			expect(votingCardIconPath.getAttribute('d')).toBe(expectedPath);
		});
	});
});
