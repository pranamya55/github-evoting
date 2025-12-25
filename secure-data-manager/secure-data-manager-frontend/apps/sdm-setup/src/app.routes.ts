/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {
	ConstituteElectoralBoardComponent,
	GenerateComponent,
	GeneratePrintFileComponent,
	PreComputeComponent,
	PreConfigureComponent,
} from '@sdm/setup';
import {DataCollectionComponent} from '@sdm/shared-feature-data-collection';
import {ExportComponent, ImportComponent,} from '@sdm/shared-feature-data-exchange';
import {SdmRoute, WorkflowStep} from '@sdm/shared-util-types';

export const appRoutes: SdmRoute[] = [
	{
		path: 'setup-1',
		children: [
			{
				path: 'pre-configure',
				component: PreConfigureComponent,
				title: 'preConfigure.title',
				data: {
					workflowStep: WorkflowStep.PreConfigure,
				},
			},
			{
				path: 'pre-compute',
				component: PreComputeComponent,
				title: 'preCompute.title',
				data: {
					workflowStep: WorkflowStep.PreCompute,
				},
			},
			{
				path: 'export-1',
				component: ExportComponent,
				title: 'dataExchange.export.title.1',
				data: {
					workflowStep: WorkflowStep.ExportToOnline1,
					exchangeIndex: '1',
				},
			},
			{path: '', redirectTo: 'export-1', pathMatch: 'full'},
		],
	},

	{
		path: 'setup-2',
		children: [
			{
				path: 'import-2',
				component: ImportComponent,
				title: 'dataExchange.import.title.2',
				data: {
					workflowStep: WorkflowStep.ImportFromOnline2,
					exchangeIndex: '2',
				},
			},
			{
				path: 'generate',
				component: GenerateComponent,
				title: 'generate.title',
				data: {
					workflowStep: WorkflowStep.Generate,
				},
			},
			{
				path: 'generate-print-file',
				component: GeneratePrintFileComponent,
				title: 'generatePrintFile.title',
				data: {
					workflowStep: WorkflowStep.GeneratePrintFile,
				},
			},
			{
				path: 'export-3',
				component: ExportComponent,
				title: 'dataExchange.export.title.3',
				data: {
					workflowStep: WorkflowStep.ExportToOnline3,
					exchangeIndex: '3',
				},
			},
			{path: '', redirectTo: 'export-3', pathMatch: 'full'},
		],
	},

	{
		path: 'setup-3',
		children: [
			{
				path: 'constitute-electoral-board',
				component: ConstituteElectoralBoardComponent,
				title: 'constituteElectoralBoard.title',
				data: {
					workflowStep: WorkflowStep.ConstituteElectoralBoard,
				},
			},
			{
				path: 'data-collect-setup',
				component: DataCollectionComponent,
				title: 'dataCollection.title.setup',
				data: {
					workflowStep: WorkflowStep.CollectDataVerifierSetup,
					mode: 'setup',
				},
			},
			{
				path: 'export-4',
				component: ExportComponent,
				title: 'dataExchange.export.title.4',
				data: {
					workflowStep: WorkflowStep.ExportToOnline4,
					exchangeIndex: '4',
				},
			},
			{path: '', redirectTo: 'export-4', pathMatch: 'full'},
		],
	},
];
