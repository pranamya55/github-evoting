/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ModalListSelectorComponent } from './modal-list-selector.component';
import { MockComponent, MockModule, MockPipe, MockProvider } from 'ng-mocks';
import { TranslatePipe } from '@ngx-translate/core';
import { SortByPipe, TranslateTextPipe } from 'e-voting-libraries-ui-kit';
import { MockList, mockSortBy, mockTranslateText } from '@vp/shared-util-testing';
import { NgbActiveModal, NgbCollapseModule } from '@ng-bootstrap/ng-bootstrap';
import { IconComponent } from '@vp/shared-ui-components';
import { By } from '@angular/platform-browser';

describe('ModalListSelectorComponent', () => {
	let component: ModalListSelectorComponent;
	let fixture: ComponentFixture<ModalListSelectorComponent>;
	let modalClose: jest.Mock;

	beforeEach(async () => {
		modalClose = jest.fn();

		await TestBed.configureTestingModule({
			imports: [MockModule(NgbCollapseModule), MockComponent(IconComponent)],
			declarations: [
				ModalListSelectorComponent,
				MockPipe(TranslatePipe, (value) => value),
				MockPipe(TranslateTextPipe, mockTranslateText),
				MockPipe(SortByPipe, mockSortBy),
			],
			providers: [MockProvider(NgbActiveModal, { close: modalClose })],
		}).compileComponents();

		fixture = TestBed.createComponent(ModalListSelectorComponent);
		component = fixture.componentInstance;

		component.list = MockList();

		fixture.detectChanges();
	});

	function getSelectListButton() {
		return fixture.debugElement.query(By.css('#select-list'));
	}

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should display the "Select" button when list is not already selected', () => {
		expect(
			getSelectListButton()
				.query(By.directive(IconComponent))
				.nativeElement.getAttribute('name'),
		).toBe('check2');
		expect(
			getSelectListButton()
				.query(By.css('span[aria-hidden="true"]'))
				.nativeElement.textContent.trim(),
		).toBe('listselection.listresult.select');
	});

	it('should call selectList() and close modal with list when button is clicked', () => {
		getSelectListButton().nativeElement.click();
		expect(modalClose).toHaveBeenNthCalledWith(1, component.list);
	});
});
