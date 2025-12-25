/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MockComponent, MockModule, MockPipe, MockProvider } from 'ng-mocks';
import { signal, Signal } from '@angular/core';
import { ReviewComponent } from './review.component';
import { ReviewElectionGroupComponent } from '../review-election-group/review-election-group.component';
import { Store } from '@ngrx/store';
import { By } from '@angular/platform-browser';
import { AccordionVoteComponent, UiComponentsModule } from '@vp/voter-portal-ui-components';
import { TranslateModule } from '@ngx-translate/core';
import { ConcatPipe, SortByPipe } from 'e-voting-libraries-ui-kit';
import { mockConcat, mockSortBy } from '@vp/shared-util-testing';
import { ConfirmationService } from '@vp/voter-portal-ui-confirmation';
import { of } from 'rxjs';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { UiDirectivesModule } from '@vp/voter-portal-ui-directives';
import { ReviewVoteQuestionComponent } from '../review-vote-question/review-vote-question.component';
import {IconComponent} from "@vp/shared-ui-components";

describe('ReviewComponent', () => {
	let component: ReviewComponent;
	let fixture: ComponentFixture<ReviewComponent>;
	let confirmationService: ConfirmationService;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				ReviewComponent,
				MockComponent(AccordionVoteComponent),
				MockComponent(ReviewVoteQuestionComponent),
				MockComponent(ReviewElectionGroupComponent),
				MockPipe(SortByPipe, mockSortBy),
				MockPipe(ConcatPipe, mockConcat),
			],
			imports: [
				MockModule(TranslateModule),
				MockModule(UiComponentsModule),
				MockModule(UiDirectivesModule),
				MockComponent(IconComponent)
			],
			providers: [
				MockProvider(Store, {
					selectSignal: <K>() => signal(<K>null) as Signal<K>,
				}),
				MockProvider(ConfirmationService, { confirm: () => of(true) }),
				MockProvider(NgbModal, {
					open: () => ({ shown: of(null) }) as unknown as NgbModalRef,
				}),
			],
		}).compileComponents();

		confirmationService = TestBed.inject(ConfirmationService);
		fixture = TestBed.createComponent(ReviewComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	describe('sealing vote', () => {
		it('should call confirmSeal on seal button click', () => {
			jest.spyOn(confirmationService, 'confirm');
			const buttonElement = fixture.debugElement.query(
				By.css('#btn_seal_vote'),
			).nativeElement;
			buttonElement.click();
			fixture.detectChanges();

			expect(confirmationService.confirm).toHaveBeenCalled();
		});
	});

	describe('buttons', () => {
		it('should show the sealing confirmation modal when the "Seal" button is clicked', () => {
			jest.spyOn(confirmationService, 'confirm');
			const buttonElement = fixture.debugElement.query(
				By.css('#btn_seal_vote'),
			).nativeElement;
			buttonElement.click();
			fixture.detectChanges();

			expect(confirmationService.confirm).toHaveBeenCalled();
		});
	});
});
