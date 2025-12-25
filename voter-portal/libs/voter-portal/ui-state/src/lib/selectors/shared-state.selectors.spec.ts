/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { SharedState } from '@vp/voter-portal-util-types';
import {
	AnswerType,
	ElectionTexts,
	Eligibility,
	VoteTexts,
} from 'e-voting-libraries-ui-kit';
import * as SharedStateSelectors from '@vp/voter-portal-ui-state';
import { initialState } from '@vp/voter-portal-ui-state';

describe('SharedState Selectors', () => {
	let state: SharedState;

	beforeEach(() => {
		state = {
			...initialState,
			error: null,
			votesTexts: [
				{
					voteIdentification: 'ch_test',
					domainOfInfluence: 'doid-ch1-mu',
					votePosition: 1,
					voteDescription: {
						DE: 'Beispielabstimmung',
						FR: "Votation d'exemple",
						IT: "Votazione d'esempio",
						RM: "votaziun d'exempel",
					},
					ballots: [
						{
							ballotIdentification: '9cbcbd59-94ad-4f48-bc7d-4bc38e5c0c51',
							ballotPosition: 1,
							questionIdentification: '806f52e6-9d49-4906-b2a8-7c89dfdf53e2',
							questionNumber: '1',
							ballotQuestion: {
								question: {
									DE: 'Wollen Sie den ersten «**Vorschlag**» annehmen?',
									FR: 'Acceptez-vous la première «**proposition**» ?',
									IT: 'accettate la prima «**proposta**» ?',
									RM: 'acceptar l’emprima «**proposta**» ?',
								},
							},
							answers: [
								{
									answerIdentification: '3aa38c9e-6e93-3159-91e1-c3da90681572',
									answerPosition: 1,
									answerType: AnswerType.YES,
									hiddenAnswer: false,
									answerInformation: {
										DE: 'Ja',
										FR: 'Oui',
										IT: 'Si',
										RM: 'Gea',
									},
								},
								{
									answerIdentification: '16ac94e3-0c72-3570-9df9-050ebb18a9cc',
									answerPosition: 2,
									answerType: AnswerType.NO,
									hiddenAnswer: false,
									answerInformation: {
										DE: 'Nein',
										FR: 'Non',
										IT: 'No',
										RM: 'Na',
									},
								},
								{
									answerIdentification: 'fbed7bf6-a7ed-3812-bb0c-22e449aa4f96',
									answerPosition: 3,
									answerType: AnswerType.EMPTY,
									hiddenAnswer: true,
									answerInformation: {
										DE: 'Leer',
										FR: 'Blanc',
										IT: 'Bianco',
										RM: 'Vid',
									},
								},
							],
						},
					],
				},
			],
			electionsTexts: [
				{
					electionGroupIdentification: 'majorz_test',
					domainOfInfluence: 'doid-ct1-mu',
					electionGroupPosition: 3,
					electionsInformation: [
						{
							election: {
								electionIdentification: 'majorz_test',
								electionDescription: {
									DE: 'Beispielwahl mit Write-Ins',
									FR: "Election d'exemple avec Write-Ins",
									IT: "Elezioni d'esempio con Write-Ins",
									RM: "Elecziuns d'exempel cun Write-Ins",
								},
								electionPosition: 1,
								numberOfMandates: 1,
								writeInsAllowed: true,
								candidateAccumulation: 1,
								minimalCandidateSelectionInList: 1,
							},
							candidates: [
								{
									candidateIdentification:
										'9bfe1f69-6d35-4966-b281-a5dc39655e3a',
									position: 1,
									eligibility: Eligibility.EXPLICIT,
									displayCandidateLine1: {
										DE: '1 Rot Doris Rote Partei',
										FR: '1 Rot Doris Rote Partei',
										IT: '1 Rot Doris Rote Partei',
										RM: '1 Rot Doris Rote Partei',
									},
									displayCandidateLine2: {
										DE: '29.11.1961, Wil',
										FR: '29.11.1961, Wil',
										IT: '29.11.1961, Wil',
										RM: '29.11.1961, Wil',
									},
								},
								{
									candidateIdentification:
										'b5c605ec-9398-41d9-b934-006c548381c3',
									position: 2,
									eligibility: Eligibility.EXPLICIT,
									displayCandidateLine1: {
										DE: '2 Gelb Henry Gelbe Partei',
										FR: '2 Gelb Henry Gelbe Partei',
										IT: '2 Gelb Henry Gelbe Partei',
										RM: '2 Gelb Henry Gelbe Partei',
									},
									displayCandidateLine2: {
										DE: '29.11.1954, Wil',
										FR: '29.11.1954, Wil',
										IT: '29.11.1954, Wil',
										RM: '29.11.1954, Wil',
									},
								},
							],
							lists: [],
							emptyList: {
								listIdentification: 'ca127938-5e2f-372f-8a4e-a0cb1da35b3b',
								listDescription: {
									DE: 'Leere Liste',
									FR: 'Leere Liste',
									IT: 'Leere Liste',
									RM: 'Leere Liste',
								},
								emptyPositions: [
									{
										emptyPositionIdentification:
											'1cde3adc-d3b8-3077-95b4-21ac2958a8fe',
										positionOnList: 1,
										emptyPositionText: {
											DE: 'Kein Name ausgewählt',
											FR: 'Aucun nom sélectionné',
											IT: 'Nessun nominativo selezionato',
											RM: 'Nagin num selecziunà',
										},
									},
								],
							},
							writeInPositions: [
								{
									writeInPositionIdentification:
										'f71ffce9-78c9-3621-895b-aebcc0849ad1',
									position: 1,
									returnCodeWriteInDescription: {
										DE: 'Return Code Write-In description',
										FR: 'Return Code Write-In description',
										IT: 'Return Code Write-In description',
										RM: 'Return Code Write-In description',
									},
								},
							],
							implicitWriteInCandidates: [],
						},
					],
				},
			],
		};
	});

	it('getVotesTexts() should return the list of VoteTexts', () => {
		const votesTexts: VoteTexts[] = <VoteTexts[]>(
			SharedStateSelectors.getVotesTexts.projector(state)
		);

		expect(votesTexts.length).toBe(1);
	});

	it('getElectionsTexts() should return the list of ElectionTexts', () => {
		const electionsTexts: ElectionTexts[] = <ElectionTexts[]>(
			SharedStateSelectors.getElectionsTexts.projector(state)
		);

		expect(electionsTexts.length).toBe(1);
	});

	it('getLoading() should return the current "loading" status', () => {
		const loading: boolean = SharedStateSelectors.getLoading.projector(state);

		expect(loading).toBe(false);
	});
});
