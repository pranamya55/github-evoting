/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { VerifyElectionGroupComponent } from './verify-election-group.component';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MockComponent, MockModule, MockPipe, MockProvider } from 'ng-mocks';
import { VerifyElectionComponent } from '../verify-election/verify-election.component';
import {
	ControlContainer,
	FormArray,
	FormBuilder,
	ReactiveFormsModule,
} from '@angular/forms';
import { SortByPipe } from 'e-voting-libraries-ui-kit';
import { By } from '@angular/platform-browser';
import { MockElectionTexts, mockSortBy } from '@vp/shared-util-testing';

describe('VerifyElectionGroupComponent', () => {
	let component: VerifyElectionGroupComponent;
	let fixture: ComponentFixture<VerifyElectionGroupComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				VerifyElectionGroupComponent,
				MockComponent(VerifyElectionComponent),
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

		fixture = TestBed.createComponent(VerifyElectionGroupComponent);
		component = fixture.componentInstance;

		component.electionTexts = MockElectionTexts();

		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should render the correct number of vp-verify-election components', () => {
		const verifyElectionComponents = fixture.debugElement.queryAll(
			By.css('vp-verify-election'),
		);
		expect(verifyElectionComponents.length).toBe(
			component.electionTexts.electionsInformation.length,
		);
	});
});
