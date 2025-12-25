/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ReviewElectionGroupComponent } from './review-election-group.component';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MockComponent, MockModule, MockPipe, MockProvider } from 'ng-mocks';
import { ReviewElectionComponent } from '../review-election/review-election.component';
import {
	ControlContainer,
	FormArray,
	FormBuilder,
	ReactiveFormsModule,
} from '@angular/forms';
import { SortByPipe } from 'e-voting-libraries-ui-kit';
import { By } from '@angular/platform-browser';
import { MockElectionTexts, mockSortBy } from '@vp/shared-util-testing';

describe('ReviewElectionGroupComponent', () => {
	let component: ReviewElectionGroupComponent;
	let fixture: ComponentFixture<ReviewElectionGroupComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				ReviewElectionGroupComponent,
				MockComponent(ReviewElectionComponent),
				MockPipe(SortByPipe, mockSortBy),
			],
			imports: [MockModule(ReactiveFormsModule)],
			providers: [
				FormBuilder,
				MockProvider(ControlContainer, {
					control: new FormArray([]),
				}),
			],
		}).compileComponents();

		fixture = TestBed.createComponent(ReviewElectionGroupComponent);
		component = fixture.componentInstance;

		component.electionTexts = MockElectionTexts();

		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should render the correct number of vp-review-election components', () => {
		const reviewElectionComponents = fixture.debugElement.queryAll(
			By.css('vp-review-election'),
		);
		expect(reviewElectionComponents.length).toBe(
			component.electionTexts.electionsInformation.length,
		);
	});
});
