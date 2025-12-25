/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ModalListComponent } from './modal-list.component';
import { MockComponent, MockPipe, MockProvider } from 'ng-mocks';
import { ClearableInputComponent } from '@vp/voter-portal-ui-components';
import {
	MockElectionInformation,
	MockList,
	mockSortBy,
	mockTranslateText,
	RandomItem,
} from '@vp/shared-util-testing';
import { TranslatePipe } from '@ngx-translate/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DestroyRef } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { List, SortByPipe, TranslateTextPipe } from 'e-voting-libraries-ui-kit';
import { ModalListSelectorComponent } from '../modal-list-selector/modal-list-selector.component';
import {IconComponent} from "@vp/shared-ui-components";

describe('ModalListComponent', () => {
	let component: ModalListComponent;
	let fixture: ComponentFixture<ModalListComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				ModalListComponent,
				MockComponent(ClearableInputComponent),
				MockComponent(ModalListSelectorComponent),
				MockPipe(TranslateTextPipe, mockTranslateText),
				MockPipe(TranslatePipe),
				MockPipe(SortByPipe, mockSortBy),
			],
			imports: [FormsModule, ReactiveFormsModule, MockComponent(IconComponent)],
			providers: [MockProvider(DestroyRef), MockProvider(NgbActiveModal)],
		})
			.overrideComponent(ModalListComponent, {
				set: {
					providers: [
						MockProvider(TranslateTextPipe, { transform: mockTranslateText }),
					],
				},
			})
			.compileComponents();

		fixture = TestBed.createComponent(ModalListComponent);
		component = fixture.componentInstance;

		const mockElectionInformation = MockElectionInformation({ hasLists: true });
		component.lists = mockElectionInformation.lists;
		component.candidates = mockElectionInformation.candidates;
		component.chosenList = RandomItem(mockElectionInformation.lists as List[]);

		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should display all candidates when no search term is provided', () => {
		component.filter.controls.displayListLine1.setValue('');

		expect(component.filteredLists.length).toEqual(component.lists.length);
	});

	it('should filter candidates based on displayCandidateLine1', () => {
		const newList: List = MockList();
		newList.displayListLine1 = {
			DE: 'Partei 01',
			FR: 'Partei 01',
			IT: 'Partei 01',
			RM: 'Partei 01',
		};
		component.lists = [...component.lists, newList];

		component.filter.controls.displayListLine1.setValue('Partei 01');

		fixture.detectChanges();

		expect(component.filteredLists.length).toEqual(1);
	});
});
