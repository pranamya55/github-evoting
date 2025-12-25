/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {validateUUID} from "../domain/validations/validations";
import {checkArgument} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {VotingServerService} from "./voting-server.service";
import {ConfigureVoterPortalResponsePayload} from "../domain/configure-voter-portal.types";
import {VotingClientTimeError} from "../domain/voting-client-time-error";

export class ConfigureVoterPortalProcess {

	private readonly votingServerService: VotingServerService;

	constructor() {
		this.votingServerService = new VotingServerService();
	}

	/**
	 * @param {string} electionEventId - the election event id.
	 * @returns {Promise<{}>} - the configureVoterPortal response expected by the Voter-Portal.
	 */
	public async configureVoterPortal(electionEventId: string): Promise<{}> {
		validateUUID(electionEventId);

		const configureVoterPortalResponsePayload: ConfigureVoterPortalResponsePayload = await this.votingServerService.configureVoterPortal(electionEventId);
		const base64Config = configureVoterPortalResponsePayload.config;
		const decodedConfig = this.atobUTF8(base64Config);
		const config = JSON.parse(decodedConfig);

		const votingServerTime = configureVoterPortalResponsePayload.votingServerTime;
		const clientSystemTime = Math.floor(Date.now() / 1000);
		if (Math.abs(votingServerTime - clientSystemTime) > 240) {
			throw new VotingClientTimeError();
		}

		checkArgument(electionEventId === configureVoterPortalResponsePayload.electionEventId,
			`The election event id in the response does not match the request. [expected: ${electionEventId}, received: ${configureVoterPortalResponsePayload.electionEventId}]`);

		// Prepare the config for the Voter Portal
		config.electionEventId = configureVoterPortalResponsePayload.electionEventId;
		config.header.logo = configureVoterPortalResponsePayload.logo;
		config.favicon = configureVoterPortalResponsePayload.favicon;

		return config;
	}

	private atobUTF8(data: string): string {
		const decodedData = atob(data);
		const utf8data = new Uint8Array(decodedData.length);
		const decoder = new TextDecoder("utf-8");
		for (let i = 0; i < decodedData.length; i++) {
			utf8data[i] = decodedData.charCodeAt(i);
		}
		return decoder.decode(utf8data);
	}

}
