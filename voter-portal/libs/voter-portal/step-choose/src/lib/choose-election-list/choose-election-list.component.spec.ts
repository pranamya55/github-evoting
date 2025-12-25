/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChooseElectionListComponent } from './choose-election-list.component';
import { MockComponent, MockPipe, MockProvider } from 'ng-mocks';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { List, TranslateTextPipe } from 'e-voting-libraries-ui-kit';
import { DestroyRef } from '@angular/core';
import { ChooseElectionComponent } from '../choose-election/choose-election.component';
import { ChooseFormService } from '../choose-form.service';
import {
	MockElectionInformation,
	mockTranslateText,
	RandomItem,
} from '@vp/shared-util-testing';
import { TranslatePipe } from '@ngx-translate/core';
import { AnswerComponent } from '@vp/voter-portal-ui-components';

const MockChooseFormService = {
	createChosenListFormGroup: jest.fn().mockReturnValue(
		new FormGroup({
			listIdentification: new FormControl(),
		}),
	),
};

describe('ChooseElectionListComponent', () => {
	let component: ChooseElectionListComponent;
	let fixture: ComponentFixture<ChooseElectionListComponent>;
	let list: List;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				ChooseElectionListComponent,
				MockComponent(AnswerComponent),
				MockPipe(TranslateTextPipe, mockTranslateText),
				MockPipe(TranslatePipe, (value) => value),
			],
			imports: [ReactiveFormsModule],
			providers: [MockProvider(DestroyRef)],
		})
			.overrideComponent(ChooseElectionComponent, {
				set: {
					providers: [MockProvider(ChooseFormService, MockChooseFormService)],
				},
			})
			.compileComponents();

		fixture = TestBed.createComponent(ChooseElectionListComponent);
		component = fixture.componentInstance;

		component.electionInformation = MockElectionInformation({ hasLists: true });
		component.chosenCandidateCount = 1;
		list = RandomItem(component.electionInformation.lists);

		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should set listIdentification when selectList is called', () => {
		component.selectList(list);

		expect(
			component.chosenListFormGroup.controls.listIdentification.value,
		).toBe(list.listIdentification);
	});

	it('should reset chosenListAnswer when unselectList is called', () => {
		component.selectList(list);
		component.unselectList();

		expect(
			component.chosenListFormGroup.controls.listIdentification.value,
		).toBe(component.electionInformation.emptyList.listIdentification);
	});

	it('should update chosenList when listIdentification value changes', () => {
		component.chosenListFormGroup.controls.listIdentification.setValue(
			list.listIdentification,
		);
		expect(component.chosenList).toEqual(list);

		component.chosenListFormGroup.controls.listIdentification.setValue(
			component.electionInformation.emptyList.listIdentification,
		);
		expect(component.chosenList).toBeUndefined();
	});
});
