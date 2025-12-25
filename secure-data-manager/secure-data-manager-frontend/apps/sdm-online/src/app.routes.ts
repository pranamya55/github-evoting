/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {
	ComputeComponent,
	ConfigureVoterPortalComponent,
	DownloadComponent,
	MixAndDownloadComponent,
	RequestCcKeysComponent,
	UploadComponent
} from '@sdm/online';
import {ExportComponent, ImportComponent} from '@sdm/shared-feature-data-exchange';
import {SdmRoute, WorkflowStep} from '@sdm/shared-util-types';

export const appRoutes: SdmRoute[] = [
	{
		path: 'online-1',
		children: [
			{
				path: 'import-1',
				component: ImportComponent,
				title: 'dataExchange.import.title.1',
				data: {
					workflowStep: WorkflowStep.ImportFromSetup1,
					exchangeIndex: '1',
				},
			},
			{
				path: 'request-cc-keys',
				component: RequestCcKeysComponent,
				title: 'requestCcKeys.title',
				data: {
					workflowStep: WorkflowStep.RequestCcKeys,
				},
			},
			{
				path: 'compute',
				component: ComputeComponent,
				title: 'compute.title',
				data: {
					workflowStep: WorkflowStep.Compute,
				},
			},
			{
				path: 'download',
				component: DownloadComponent,
				title: 'download.title',
				data: {
					workflowStep: WorkflowStep.Download,
				},
			},
			{
				path: 'export-2',
				component: ExportComponent,
				title: 'dataExchange.export.title.2',
				data: {
					workflowStep: WorkflowStep.ExportToSetup2,
					exchangeIndex: '2',
				},
			},
			{path: '', redirectTo: 'export-2', pathMatch: 'full'},
		],
	},

	{
		path: 'online-2',
		children: [
			{
				path: 'import-3',
				component: ImportComponent,
				title: 'dataExchange.import.title.3',
				data: {
					workflowStep: WorkflowStep.ImportFromSetup3,
					exchangeIndex: '3',
				},
			},
			{
				path: 'upload-1',
				component: UploadComponent,
				title: 'upload.title.1',
				data: {
					workflowStep: WorkflowStep.UploadConfiguration1,
					day: 1,
				},
			},
			{path: '', redirectTo: 'upload-1', pathMatch: 'full'},
		],
	},

	{
		path: 'online-3',
		children: [
			{
				path: 'import-4',
				component: ImportComponent,
				title: 'dataExchange.import.title.4',
				data: {
					workflowStep: WorkflowStep.ImportFromSetup4,
					exchangeIndex: '4',
				},
			},
			{
				path: 'upload-2',
				component: UploadComponent,
				title: 'upload.title.2',
				data: {
					workflowStep: WorkflowStep.UploadConfiguration2,
					day: 2,
				},
			},
			{
				path: 'configure-voter-portal',
				component: ConfigureVoterPortalComponent,
				title: 'configure-voter-portal.title',
				data: {
					workflowStep: WorkflowStep.ConfigureVoterPortal,
				},
			},
			{path: '', redirectTo: 'configure-voter-portal', pathMatch: 'full'},
		],
	},

	{
		path: 'online-4',
		children: [
			{
				path: 'mix-and-download',
				component: MixAndDownloadComponent,
				title: 'mixAndDownload.title',
				data: {
					workflowStep: WorkflowStep.MixAndDownload,
				},
			},
			{
				path: 'export-5',
				component: ExportComponent,
				title: 'dataExchange.export.title.5',
				data: {
					workflowStep: WorkflowStep.ExportToTally5,
					exchangeIndex: '5',
				},
			},
			{path: '', redirectTo: 'mix-and-download', pathMatch: 'full'},
		],
	},
];
