/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
export type FooterCancelMode = Omit<CancelMode, CancelMode.QuitProcess> | false;

export enum CancelMode {
	CancelVote = 'CancelVote',
	LeaveProcess = 'LeaveProcess',
	QuitProcess = 'QuitProcess',
}
