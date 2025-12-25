/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Observable} from "rxjs";
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {ResultsService} from "../results.service";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {Component, DestroyRef, inject, Input, OnInit} from '@angular/core';
import {BallotBoxResults} from "e-voting-libraries-ui-kit";

@Component({
	selector: 'sdm-results-modal',
	templateUrl: './results-modal.component.html',
	standalone: false
})
export class ResultsModalComponent implements OnInit {
	@Input() ballotBoxId!: string;
	ballotBoxResults$!: Observable<BallotBoxResults>;
	readonly activeModal = inject(NgbActiveModal);
	private readonly resultsService = inject(ResultsService);
	private readonly destroyRef = inject(DestroyRef);

	ngOnInit(): void {
		this.ballotBoxResults$ = this.resultsService.getBallotBoxResults(this.ballotBoxId).pipe(
			takeUntilDestroyed(this.destroyRef)
		);
	}

}
