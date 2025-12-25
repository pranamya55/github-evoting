/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {SendVoteResponse} from "./domain/send-vote.types";
import {ConfirmVoteResponse} from "./domain/confirm-vote.types";
import {AuthenticateVoterResponse} from "./domain/authenticate-voter.types";

/**
 * Wraps the worker calls and responses with a promise.
 */

export class WorkerWrapper {
	private static instance: WorkerWrapper;
	worker: Worker;

	constructor() {
		// Create the worker with its fixed bundle name (see Voter-Portal build configuration)
		this.worker = new Worker("./crypto.ov-worker.js");
		this.worker.onmessage = handleResponseFromWorker;
	}

	/**
	 * Provides a fresh new instance of WorkerWrapper.
	 * @returns {WorkerWrapper}
	 */
	static getInstance(): WorkerWrapper {
		if (!WorkerWrapper.instance) {
			WorkerWrapper.instance = new WorkerWrapper();
		}
		return WorkerWrapper.instance;
	}

	invoke(operation: string): Promise<WorkerWrapperResponse> {
		return sendRequestToWorker(this.worker, {
			operation: operation,
			args: [].slice.call(arguments, 1)
		});
	}
}

export type WorkerWrapperResponse = AuthenticateVoterResponse | ConfirmVoteResponse | SendVoteResponse;

export type WorkerRequestPayload = {
	operation: string,
	args: []
}

const callbacks = {
	resolves: {},
	rejects: {}
};


/**
 * Sends a request to the worker api.
 * @param {Worker} worker - the worker object who expect request.
 * @param {WorkerRequestPayload} payload - the payload to send.
 * @returns {Promise<unknown>|void}
 */
function sendRequestToWorker(worker: Worker, payload: WorkerRequestPayload): Promise<WorkerWrapperResponse> {
	const {operation} = payload;
	return new Promise(function (resolve, reject) {
		// Save promise callbacks for worker operation response.
		callbacks.resolves[operation] = resolve;
		callbacks.rejects[operation] = reject;
		// Send request to the worker.
		worker.postMessage(payload);
	});
}

/**
 * Handles a worker response.
 * @param {CustomMessageEvent} workerMessage - the message received from the worker.
 * @returns {Promise<unknown>|unknown}
 */
function handleResponseFromWorker(workerMessage: CustomMessageEvent): void {
	const {operation, result, error} = workerMessage.data;
	if (result) {
		const resolve = callbacks.resolves[operation];
		if (resolve) {
			resolve(result);
		} else {
			throw new Error(`Unhandled worker response [operation: ${operation}]`);
		}
    } else {
		const reject = callbacks.rejects[operation];
		if (reject) {
			reject(error);
		} else {
			throw new Error(`Unhandled worker error [operation: ${operation}]`);
		}
	}

	// Delete callbacks
	delete callbacks.resolves[operation];
	delete callbacks.rejects[operation];
}

interface WorkerResponsePayload {
	operation: string;
	result?: any;
	error?: any;
}

interface CustomMessageEvent extends MessageEvent {
	data: WorkerResponsePayload;
}