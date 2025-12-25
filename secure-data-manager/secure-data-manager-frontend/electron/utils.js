/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

const getModeConfig = (mode) => {
	switch (mode) {
		case 'LAUNCHER':
			return {
				mode: 'LAUNCHER',
				app: 'sdm-launcher',
				clientUrl: 'http://localhost:4203/',
			};
		case 'SETUP':
			return {
				mode: 'SETUP',
				app: 'sdm-setup',
				clientUrl: 'http://localhost:4200/',
				backendUrl: 'http://localhost:8090',
				debugPort: '6090',
			};
		case 'ONLINE':
			return {
				mode: 'ONLINE',
				app: 'sdm-online',
				clientUrl: 'http://localhost:4202/',
				backendUrl: 'http://localhost:8092',
				debugPort: '6092',
			};
		case 'TALLY':
			return {
				mode: 'TALLY',
				app: 'sdm-tally',
				clientUrl: 'http://localhost:4201/',
				backendUrl: 'http://localhost:8091',
				debugPort: '6091',
			};
	}
	throw Error('Application mode is not valid.');
};

// Map a raw settings to a UI settings
const convertRawSettingsToUISettings = (rawSettings) => {
	let directTrustLocation = '';
	let directTrustPwdLocation = '';
	if (rawSettings.mode === 'SETUP' || rawSettings.mode === 'TALLY') {
		directTrustLocation = getValueFromKey(
			rawSettings.properties,
			'direct-trust.keystore.location',
		);
		directTrustPwdLocation = getValueFromKey(
			rawSettings.properties,
			'direct-trust.password.location',
		);
	}
	let outputFolder = '';
	if (rawSettings.mode === 'SETUP' || rawSettings.mode === 'ONLINE') {
		outputFolder = getValueFromKey(
			rawSettings.properties,
			'sdm.path.export',
		);
	}
	let tallyFolder = '';
	if (rawSettings.mode === 'TALLY') {
		tallyFolder = getValueFromKey(
			rawSettings.properties,
			'sdm.path.tally',
		);
	}

	return {
		id: rawSettings.id,
		mode: rawSettings.mode,
		electionEventSeed: getValueFromKey(
			rawSettings.properties,
			'sdm.election-event-seed',
		),
		voterPortalHost: getValueFromKey(
			rawSettings.properties,
			'voter-portal.host'
		),
		votingServerHost: getValueFromKey(
			rawSettings.properties,
			'voting-server.host'
		),
		twoWaySSLLocation: getValueFromKey(
			rawSettings.properties,
			'voting-server.connection.client-keystore-location',
		),
		twoWaySSLPwdLocation: getValueFromKey(
			rawSettings.properties,
			'voting-server.connection.client-keystore-password-location',
		),
		voterPortalConfigurationFolder: getValueFromKey(rawSettings.properties, 'sdm.path.voter-portal-configuration'),
		outputFolder: outputFolder,
		externalConfigurationFolder: getValueFromKey(
			rawSettings.properties,
			'sdm.path.external-configuration',
		),
		printFolder: getValueFromKey(
			rawSettings.properties,
			'sdm.path.print',
		),
		verifierFolder: getValueFromKey(
			rawSettings.properties,
			'sdm.path.verifier',
		),
		tallyFolder	: tallyFolder,
		workspaceFolder: getValueFromKey(rawSettings.properties, 'sdm.path.workspace'),
		exportPwd: getValueFromKey(
			rawSettings.properties,
			'sdm.process.data-exchange.zip-password',
		),
		verifierDatasetPwd: getValueFromKey(
			rawSettings.properties,
			'sdm.process.collect-data-verifier.zip-password',
		),
		choiceCodeGenerationChunkSize: getValueFromKey(
			rawSettings.properties,
			'sdm.process.precompute.genVerDat-chunk-size',
		),
		directTrustLocation: directTrustLocation,
		directTrustPwdLocation: directTrustPwdLocation,
		proxyConfig: getValueFromKey(
			rawSettings.properties,
			'proxy.config',
		)
	};
};

function getValueFromKey(properties, key) {
	let property = properties.find((prop) => prop.key === key);
	if (property) {
		return property.value;
	}
	return null;
}

// Map a UI settings to a raw settings
const convertUISettingsToRawSettings = (UISettings) => {
	const rawSettings = {};

	// Sanitize id
	if (isNaN(UISettings.id)) {
		throw Error('Settings id must be a number.');
	}
	rawSettings.id = UISettings.id;

	// Sanitize mode
	rawSettings.mode = getModeConfig(UISettings.mode).mode;

	// Properties
	rawSettings.properties = [];

	// Common properties
	rawSettings.properties.push({
		key: 'sdm.election-event-seed',
		value: UISettings.electionEventSeed,
	});
	rawSettings.properties.push({
		key: 'sdm.path.workspace',
		value: UISettings.workspaceFolder,
	});
	rawSettings.properties.push({
		key: 'sdm.path.export',
		value: UISettings.outputFolder,
	});
	rawSettings.properties.push({
		key: 'sdm.process.data-exchange.zip-password',
		value: UISettings.exportPwd,
	});
	rawSettings.properties.push({
		key: 'sdm.process.collect-data-verifier.zip-password',
		value: UISettings.verifierDatasetPwd,
	});

	if (UISettings.mode === 'ONLINE') {
		// Only for Online
		rawSettings.properties.push({key: 'server.port', value: '8092'});
		rawSettings.properties.push({key: 'role.isSetup', value: 'false'});
		rawSettings.properties.push({key: 'role.isTally', value: 'false'});
		rawSettings.properties.push({
			key: 'voter-portal.host',
			value: UISettings.voterPortalHost,
		});
		rawSettings.properties.push({
			key: 'voting-server.host',
			value: UISettings.votingServerHost,
		});
		rawSettings.properties.push({
			key: 'sdm.path.voter-portal-configuration',
			value: UISettings.voterPortalConfigurationFolder,
		});
		rawSettings.properties.push({key: 'proxy.config', value: UISettings.proxyConfig});
		if (
			UISettings.twoWaySSLLocation &&
			UISettings.twoWaySSLLocation.trim() !== '' &&
			UISettings.twoWaySSLPwdLocation &&
			UISettings.twoWaySSLPwdLocation.trim() !== ''
		) {
			rawSettings.properties.push({
				key: 'voting-server.connection.client-keystore-location',
				value: UISettings.twoWaySSLLocation,
			});
			rawSettings.properties.push({
				key: 'voting-server.connection.client-keystore-password-location',
				value: UISettings.twoWaySSLPwdLocation,
			});
		}
	} else {
		// Common to Setup & Tally
		rawSettings.properties.push({
			key: 'sdm.path.verifier',
			value: UISettings.verifierFolder,
		});

		if (UISettings.mode === 'SETUP') {
			// Only for Setup
			rawSettings.properties.push({key: 'server.port', value: '8090'});
			rawSettings.properties.push({key: 'role.isSetup', value: 'true'});
			rawSettings.properties.push({key: 'role.isTally', value: 'false'});
			rawSettings.properties.push({
				key: 'direct-trust.keystore.location',
				value: UISettings.directTrustLocation,
			});
			rawSettings.properties.push({
				key: 'direct-trust.password.location',
				value: UISettings.directTrustPwdLocation,
			});
			rawSettings.properties.push({
				key: 'sdm.process.precompute.genVerDat-chunk-size',
				value: UISettings.choiceCodeGenerationChunkSize,
			});
			rawSettings.properties.push({
				key: 'sdm.path.external-configuration',
				value: UISettings.externalConfigurationFolder,
			});
			rawSettings.properties.push({
				key: 'sdm.path.print',
				value: UISettings.printFolder,
			});
		} else {
			// Only for Tally
			rawSettings.properties.push({key: 'server.port', value: '8091'});
			rawSettings.properties.push({key: 'role.isSetup', value: 'false'});
			rawSettings.properties.push({key: 'role.isTally', value: 'true'});
			rawSettings.properties.push({
				key: 'direct-trust.keystore.location',
				value: UISettings.directTrustLocation,
			});
			rawSettings.properties.push({
				key: 'direct-trust.password.location',
				value: UISettings.directTrustPwdLocation,
			});
			rawSettings.properties.push({
				key: 'sdm.path.tally',
				value: UISettings.tallyFolder,
			});
		}
	}

	return rawSettings;
};

module.exports = {
	getModeConfig: getModeConfig,
	convertToUISettings: convertRawSettingsToUISettings,
	convertToRawSettings: convertUISettingsToRawSettings,
};
