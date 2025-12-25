/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormsModule } from '@angular/forms';
import { provideMockStore } from '@ngrx/store/testing';
import { FAQService } from '@vp/voter-portal-feature-faq';
import { MockProvider } from 'ng-mocks';
import { TranslateTestingModule } from 'ngx-translate-testing';
import { ChooseElectionCandidateWriteInComponent } from './choose-election-candidate-write-in.component';
import { MockElectionInformation, RandomItem } from '@vp/shared-util-testing';

describe('ChooseElectionCandidateWriteInComponent', () => {
	let component: ChooseElectionCandidateWriteInComponent;
	let fixture: ComponentFixture<ChooseElectionCandidateWriteInComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [ChooseElectionCandidateWriteInComponent],
			providers: [provideMockStore({}), MockProvider(FAQService)],
			imports: [TranslateTestingModule.withTranslations({}), FormsModule],
		}).compileComponents();
	});

	beforeEach(() => {
		fixture = TestBed.createComponent(ChooseElectionCandidateWriteInComponent);
		component = fixture.componentInstance;
		component.electionInformation = MockElectionInformation({
			writeInsAllowed: true,
		});
		component.emptyPosition = RandomItem(
			component.electionInformation.emptyList.emptyPositions,
		);
		component.writeInControl = new FormControl();
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});
});
