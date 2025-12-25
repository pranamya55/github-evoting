/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {
	AfterContentInit,
	Component,
	DestroyRef,
	EventEmitter,
	inject,
	Input,
	Output,
} from '@angular/core';
import { ConfirmationModalConfig, FormGroupFrom } from '@vp/voter-portal-util-types';
import {
	ChosenList,
	ElectionInformation,
	List,
} from 'e-voting-libraries-ui-kit';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ChooseFormService } from '../choose-form.service';
import { filter } from 'rxjs/operators';
import { ModalListComponent } from '../modal-list/modal-list.component';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ConfirmationService } from '@vp/voter-portal-ui-confirmation';

@Component({
	selector: 'vp-choose-election-list',
	templateUrl: './choose-election-list.component.html',
	providers: [ChooseFormService],
	standalone: false,
})
export class ChooseElectionListComponent implements AfterContentInit {
	@Input({ required: true }) electionInformation!: ElectionInformation;
	@Input({ required: true }) chosenCandidateCount!: number;
	@Output() listSelected: EventEmitter<List> = new EventEmitter();

	chosenList?: List;
	chosenListFormGroup!: FormGroupFrom<ChosenList>;

	private readonly modalService: NgbModal = inject(NgbModal);
	private readonly destroyRef: DestroyRef = inject(DestroyRef);
	private readonly chooseFormService: ChooseFormService =
		inject(ChooseFormService);
	private readonly confirmationService: ConfirmationService =
		inject(ConfirmationService);

	ngAfterContentInit(): void {
		this.chosenListFormGroup = this.chooseFormService.createChosenListFormGroup(
			this.electionInformation,
		);
		this.observeChosenListChange();
	}

	selectList(list: List): void {
		this.chosenListFormGroup.controls.listIdentification.setValue(
			list.listIdentification,
		);
		this.listSelected.emit(list);
	}

	unselectList(): void {
		this.chosenListFormGroup.reset();
		this.listSelected.emit(undefined);
	}

	openListSelectionModal(): void {
		const modalOptions = { fullscreen: 'xl', size: 'xl' };
		const modalRef = this.modalService.open(ModalListComponent, modalOptions);

		Object.assign(modalRef.componentInstance, {
			lists: this.electionInformation.lists,
			candidates: this.electionInformation.candidates,
			chosenList: this.chosenList,
		});

		modalRef.result
			.then((chosenList: List) => {
				const showConfirmModal: boolean =
					this.chosenList !== undefined || this.chosenCandidateCount > 0;
				if (showConfirmModal) {
					this.confirmListChange(chosenList);
				} else {
					this.selectList(chosenList);
				}
			})
			.catch(() => {
				/* do nothing when modal is dismissed */
			});
	}

	clearList(): void {
		const deletionModalConfig: ConfirmationModalConfig = {
			content: 'listandcandidates.clearlistquestion.text',
			confirmLabel: 'listandcandidates.clearlistquestion.yes',
		};

		this.confirmationService
			.confirm(deletionModalConfig)
			.pipe(filter((deletionConfirmed) => deletionConfirmed))
			.subscribe(() => this.unselectList());
	}

	private confirmListChange(newList: List): void {
		const changeModalConfig = {
			content: 'listandcandidates.changelistquestion.text',
			confirmLabel: 'listandcandidates.changelistquestion.yes',
		};

		this.confirmationService
			.confirm(changeModalConfig)
			.pipe(filter((changeConfirmed) => changeConfirmed))
			.subscribe(() => {
				this.selectList(newList);
			});
	}

	private observeChosenListChange(): void {
		const { listIdentification } = this.chosenListFormGroup.controls;
		listIdentification.valueChanges
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe((listIdentification) => {
				if (
					listIdentification ===
					this.electionInformation.emptyList.listIdentification
				) {
					this.chosenList = undefined;
					return;
				}

				this.chosenList = this.electionInformation.lists.find(
					(list) => list.listIdentification === listIdentification,
				);
			});
	}
}
