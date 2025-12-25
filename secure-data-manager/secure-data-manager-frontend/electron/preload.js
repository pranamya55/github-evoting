/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
const fs = require('fs');
const path = require('path');
const {contextBridge, ipcRenderer} = require('electron');
const {join} = require('path');
const {
	convertToRawSettings, convertToUISettings, getModeConfig,
} = require('./utils');

// Read the settings.json file.
function readRawSettingsJson() {
	const file = path.join(__dirname, './settings.json');
	const rawSettings = fs.readFileSync(file, 'utf8');
	return JSON.parse(rawSettings);
}

/**
 * Provides a list of Settings from the settings.json file.
 * @returns {Settings[]}
 */
function getUISettingsList() {
	const rawSettingsList = readRawSettingsJson();
	const settingsList = [];

	for (let index in rawSettingsList) {
		const uiSettings = convertToUISettings(rawSettingsList[index]);
		const enhancedUiSettings = addElectionEventInfo(uiSettings);
		settingsList.push(enhancedUiSettings);
	}

	return settingsList;
}

// Add election event info to the UI settings.
function addElectionEventInfo(settings) {
	const sdmWorkspacePath = settings.workspaceFolder;
	const electionEventIdDirectoriesNames = fs.readdirSync(sdmWorkspacePath).filter(dir => dir.length === 32 && /^[A-Za-z0-9]+$/.test(dir));

	if (electionEventIdDirectoriesNames.length === 0) {
		return settings;
	}

	if (electionEventIdDirectoriesNames.length > 1) {
		console.warn('More than one election event directory found. Using the first one.');
	}

	const filePath = path.join(sdmWorkspacePath, electionEventIdDirectoriesNames[0], '/electionEventContextPayload.json')
	try {
		const data = fs.readFileSync(filePath, 'utf-8');
		const json = JSON.parse(data);

		if (settings.electionEventSeed === json.seed) {
			settings.electionEventDescription = json.electionEventContext.electionEventDescription;
			settings.electionEventId = json.electionEventContext.electionEventId;
			return settings;
		}

	} catch (err) {
		if (err.code === 'ENOENT') {
			console.error(`The file ${filePath} does not exist.`);
		} else {
			console.error('An error occurred:', err);
		}
	}
	return settings;
}

// Save UI settings to file based on the id. Start the SDM backend if startSDM is true.
function saveUISettings(UISettings, startSDM) {
	const convertedRawSettings = convertToRawSettings(UISettings);

	const rawSettingsList = readRawSettingsJson();
	// If id is not set, set it to the next available id
	if (UISettings.id === -1) {
		convertedRawSettings.id = rawSettingsList.length;
	}
	rawSettingsList[convertedRawSettings.id] = convertedRawSettings;

	const file = path.join(__dirname, `./settings.json`);
	fs.writeFile(file, JSON.stringify(rawSettingsList), (err) => {
		if (err) console.log(err);
		if (startSDM) {
			writeApplicationProperties(convertedRawSettings);
		}
	});
}

// Delete a UI settings based on the id.
function deleteUISettings(id) {
	const rawSettingsList = readRawSettingsJson();
	let newIndex = 0;
	let result = [];
	for (let index in rawSettingsList) {
		if (rawSettingsList[index].id !== id) {
			rawSettingsList[index].id = newIndex;
			result[newIndex] = rawSettingsList[index];
			newIndex++;
		}
	}

	const file = path.join(__dirname, `./settings.json`);
	fs.writeFile(file, JSON.stringify(result), (err) => {
		if (err) console.log(err);
	});
}

// Write application properties to file.
function writeApplicationProperties(rawSettings) {
	let applicationProperties = '';
	let proxyCmd = "";
	const debugConfig = readDebugConfig();

	// Override raw settings properties with debug properties.
	rawSettings.properties.forEach((rawProperty) => {
		const debugProperty = debugConfig.properties.find((debugProperty) => debugProperty.key === rawProperty.key);

		// Check if the property is a proxy config to generate the proxy command.
		// If not, add the property to the properties string.
		if (rawProperty.key === 'proxy.config') {
			proxyCmd = generateProxyCommand(debugProperty ? debugProperty.value : rawProperty.value);
		} else {
			applicationProperties += preparePropertyLine(rawProperty.key, debugProperty ? debugProperty.value : rawProperty.value);
		}
	});

	// Add debug properties that are not in the raw settings.
	debugConfig.properties.forEach((debugProperty) => {
		const debugPropertyFound = rawSettings.properties.find((rawProperty) => rawProperty.key === debugProperty.key,);

		if (!debugPropertyFound) {
			// Check if the property is a proxy config to generate the proxy command.
			// If not, add the property to the properties string.
			if (debugProperty.key === 'proxy.config') {
				proxyCmd = generateProxyCommand(debugProperty.value);
			} else {
				applicationProperties += preparePropertyLine(debugProperty.key, debugProperty.value);
			}
		}
	});

	// Write properties to file
	const file = join(__dirname, '..', '..', `./application-profile_${rawSettings.id}.properties`,);
	fs.writeFile(file, applicationProperties, (err) => {
		if (err) console.log(err);
		startupSdmBackend(rawSettings.mode, rawSettings.id, proxyCmd);
	});
}

// Generate a CLI proxy command.
function generateProxyCommand(proxyConfig) {
	return ('\n' + proxyConfig).split('\n').reduce((acc, line) => {
		if (line.length > 0) {
			return acc + ' -D' + line
		} else {
			return acc;
		}
	});
}

// Prepare a property line. If the value is a string, escape backslashes.
function preparePropertyLine(key, value) {
	if (typeof value === 'string') {
		value = value.replace(/\\/g, '\\\\');
	}
	return key + '=' + value + '\n';
}

// Start the SDM backend.
function startupSdmBackend(mode, id, proxy) {
	const debugConfig = readDebugConfig();
	const reply = ipcRenderer.sendSync('startup-sdm-backend', {
		mode: mode, profileId: id, debug: debugConfig.debug, useWindow: debugConfig.useWindow, proxy: proxy
	});
	if (reply.pid !== null) {
		checkBackend(mode);
	} else {
		console.error('Backend not started.');
	}
}

// Check if the backend is ready. If not, retry after 3 seconds.
function checkBackend(mode) {
	const modeConfig = getModeConfig(mode);
	setTimeout(() => {
		fetch(`${modeConfig.backendUrl}/actuator/health`, {method: 'GET'}).then((response) => {
			// network error in the 4xx–5xx range
			if (!response.ok) {
				console.error('Backend server error response (4xx–5xx).', response);
				// recursive check
				checkBackend(mode);
			}
			response.json().then((data) => {
				if (data.status === 'UP') {
					console.log('Backend server is ready.');
					startupSdmFrontend(mode);
				} else {
					console.error('Ready but not UP.', data);
				}
			});
		}, (error) => {
			console.error('Backend server is not reachable (error on fetch).', error,);
			// recursive check
			checkBackend(mode);
		},);
	}, 3000);
}

// Start the SDM frontend.
function startupSdmFrontend(mode) {
	const debugConfig = readDebugConfig();
	ipcRenderer.sendSync('startup-sdm-frontend', {
		mode: mode, useWindow: debugConfig.useWindow,
	});
}

// Read debug config.
function readDebugConfig() {
	const file = path.join(__dirname, './debug-config.json');
	const debug = fs.readFileSync(file, 'utf8');
	return JSON.parse(debug);
}

// Check if a path is valid.
function isValidPath(path) {
	return fs.existsSync(path);
}

// Exposed API
contextBridge.exposeInMainWorld('SettingsApi', {
	deleteSettings: deleteUISettings,
	getSettingsList: getUISettingsList,
	isValidPath: isValidPath,
	saveSettings: saveUISettings,
});
