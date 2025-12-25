/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, DestroyRef, inject, Input, OnChanges, SimpleChanges} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {ActivatedRouteSnapshot} from '@angular/router';
import {concatLatestFrom} from '@ngrx/operators';
import {TranslateService} from '@ngx-translate/core';
import {VotingStep} from '@vp/voter-portal-util-types';
import {Subscription} from 'rxjs';
import {ConfigurationService} from '@vp/voter-portal-ui-services';
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {VotingCardIconName} from "@vp/shared-ui-components";

@Component({
	selector: 'vp-stepper',
	templateUrl: './stepper.component.html',
	standalone: false
})
export class StepperComponent implements OnChanges {
	private readonly translate = inject(TranslateService);
	private readonly titleService = inject(Title);
	private readonly destroyRef = inject(DestroyRef);
	protected readonly configuration = inject(ConfigurationService);

	@Input({required: true}) childRouteSnapshot!: ActivatedRouteSnapshot;
	steps = Object.values(VotingStep);
	currentStep: string | undefined;
	currentStepIndex: number | undefined;

	votingStepSymbol: { [step in VotingStep]?: VotingCardIconName } = {
		[VotingStep.StartVoting]: 'voting-card-triangle',
		[VotingStep.Verify]: 'voting-card-diamond',
		[VotingStep.Cast]: 'voting-card-pentagon',
		[VotingStep.Confirm]: 'voting-card-star',
	};

	private pageTitleSubscription?: Subscription;

	ngOnChanges(changes: SimpleChanges) {
		const currentStep = this?.childRouteSnapshot?.url[0] ? this.childRouteSnapshot?.url[0].path : undefined;

		this.setCurrentStep(currentStep);
		this.setPageTitle(currentStep);
		this.setFirstFocus();
	}

	protected readonly VotingStep = VotingStep;

	private setFirstFocus(): void {
		window.requestAnimationFrame(() => {
			const stepper = document.querySelector<HTMLElement>(
				'#voting-progress-stepper [aria-current="step"]'
			);

			if (stepper) {
				stepper.focus();
			}
		});
	}

	private setPageTitle(currentStep: string | undefined) {
		if (!currentStep) return;

		if (this.pageTitleSubscription) this.pageTitleSubscription.unsubscribe();

		this.pageTitleSubscription = this.translate.stream(`stepper.${currentStep}`).pipe(
			concatLatestFrom(() => this.translate.get('common.pageTitle')),
			takeUntilDestroyed(this.destroyRef)
		).subscribe(([stepName, pageTitle]) => {
			this.titleService.setTitle(
				pageTitle + (stepName ? ` - ${stepName}` : '')
			);
		});
	}

	private setCurrentStep(currentStep: string | undefined) {
		this.currentStep = currentStep;
		this.currentStepIndex = this.steps.findIndex(
			(step) => step === currentStep
		);
	}
}
