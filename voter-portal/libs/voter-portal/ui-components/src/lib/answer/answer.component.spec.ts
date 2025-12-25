/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { AnswerComponent } from './answer.component';

@Component({
	standalone: false,
	template: `
		<vp-answer>
			<input type="checkbox" />
		</vp-answer>
	`,
})
class TestingHostComponent {}

describe('AnswerComponent', () => {
	let component: TestingHostComponent;
	let answer: DebugElement;
	let fixture: ComponentFixture<TestingHostComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [TestingHostComponent, AnswerComponent],
		}).compileComponents();

		fixture = TestBed.createComponent(TestingHostComponent);
		component = fixture.componentInstance;

		answer = fixture.debugElement.query(By.directive(AnswerComponent));

		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should bind isActive to class active', () => {
		answer.componentInstance.isActive = true;
		fixture.detectChanges();
		expect(answer.classes['active']).toBeTruthy();

		answer.componentInstance.isActive = false;
		fixture.detectChanges();
		expect(answer.classes['active']).toBeFalsy();
	});

	describe('click handling', () => {
		let formControl: DebugElement;

		beforeEach(() => {
			formControl = answer.query(By.css('input'));
			jest.spyOn(formControl.nativeElement, 'click');
		});

		it('should trigger a click on the radio button when the component is clicked', () => {
			answer.nativeElement.click();
			expect(formControl.nativeElement.click).toHaveBeenCalledTimes(1);
		});

		it('should trigger a single click when the form control is clicked', () => {
			formControl.nativeElement.click();
			expect(formControl.nativeElement.click).toHaveBeenCalledTimes(1);
		});
	});
});
