/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormArray, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MockComponent, MockModule, MockPipe, MockProvider } from 'ng-mocks';
import { ChooseComponent } from './choose.component';
import { ChooseVoteComponent } from '../choose-vote/choose-vote.component';
import { ChooseElectionGroupComponent } from '../choose-election-group/choose-election-group.component';
import { By } from '@angular/platform-browser';
import {
	mockConcat,
	MockElectionTexts,
	mockSortBy,
	MockStoreProvider,
	MockVoteTexts,
	RandomArray,
} from '@vp/shared-util-testing';
import {
	ConcatPipe,
	ElectionTexts,
	SortByPipe,
	VoteTexts,
} from 'e-voting-libraries-ui-kit';
import { FooterComponentComponent } from '@vp/voter-portal-ui-components';
import { TranslateModule } from '@ngx-translate/core';
import { ChooseFormService } from '../choose-form.service';
import {IconComponent} from "@vp/shared-ui-components";

class MockState {
	votesTexts: VoteTexts[];
	electionsTexts: ElectionTexts[];

	constructor() {
		this.votesTexts = RandomArray(MockVoteTexts);
		this.electionsTexts = RandomArray(MockElectionTexts);
	}
}

const MockChooseFormService = {
	createVoterAnswersFormGroup: jest.fn().mockReturnValue(
		new FormGroup({
			voteAnswers: new FormArray([]),
			electionAnswers: new FormArray([]),
		}),
	),
};

describe('ChooseComponent', () => {
	let component: ChooseComponent;
	let fixture: ComponentFixture<ChooseComponent>;
	let initialState: MockState;

	beforeEach(async () => {
		initialState = new MockState();

		await TestBed.configureTestingModule({
			declarations: [
				ChooseComponent,
				MockComponent(ChooseVoteComponent),
				MockComponent(ChooseElectionGroupComponent),
				MockComponent(FooterComponentComponent),
				MockPipe(SortByPipe, mockSortBy),
				MockPipe(ConcatPipe, mockConcat),
			],
			imports: [ReactiveFormsModule, MockModule(TranslateModule),
				MockComponent(IconComponent)],
			providers: [MockStoreProvider(initialState)],
		})
			.overrideComponent(ChooseComponent, {
				set: {
					providers: [MockProvider(ChooseFormService, MockChooseFormService)],
				},
			})
			.compileComponents();

		fixture = TestBed.createComponent(ChooseComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should render the vp-choose-vote component when there are votes', () => {
		const voteComponents = fixture.debugElement.queryAll(
			By.css('vp-choose-vote'),
		);
		expect(voteComponents).toHaveLength(initialState.votesTexts.length);
	});

	it('should render the vp-choose-election-group component when there are election groups', () => {
		const electionGroupComponents = fixture.debugElement.queryAll(
			By.css('vp-choose-election-group'),
		);
		expect(electionGroupComponents).toHaveLength(
			initialState.electionsTexts.length,
		);
	});
});
