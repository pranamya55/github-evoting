/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ChooseElectionGroupComponent } from './choose-election-group.component';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MockComponent, MockModule, MockPipe, MockProvider } from 'ng-mocks';
import { ChooseElectionComponent } from '../choose-election/choose-election.component';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { mockSortBy } from '@vp/shared-util-testing';
import { ChooseFormService } from '../choose-form.service';
import { ElectionTexts, SortByPipe } from 'e-voting-libraries-ui-kit';

const MockChooseFormService = {
	createElectionAnswersFormGroup: jest.fn().mockReturnValue(new FormGroup({})),
};

describe('ChooseElectionGroupComponent', () => {
	let component: ChooseElectionGroupComponent;
	let fixture: ComponentFixture<ChooseElectionGroupComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				ChooseElectionGroupComponent,
				MockComponent(ChooseElectionComponent),
				MockPipe(SortByPipe, mockSortBy),
			],
			imports: [MockModule(ReactiveFormsModule)],
		})
			.overrideComponent(ChooseElectionComponent, {
				set: {
					providers: [MockProvider(ChooseFormService, MockChooseFormService)],
				},
			})
			.compileComponents();

		fixture = TestBed.createComponent(ChooseElectionGroupComponent);
		component = fixture.componentInstance;

		component.electionTexts = {
			electionGroupIdentification: 'test-election-group-id',
			electionGroupPosition: 1,
			electionsInformation: [
				{ election: { electionPosition: 1 } },
				{ election: { electionPosition: 2 } },
			],
		} as ElectionTexts;

		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should render the correct number of vp-choose-election components', () => {
		const chooseElectionComponents = fixture.debugElement.queryAll(
			By.css('vp-choose-election'),
		);
		expect(chooseElectionComponents.length).toBe(
			component.electionTexts.electionsInformation.length,
		);
	});
});
