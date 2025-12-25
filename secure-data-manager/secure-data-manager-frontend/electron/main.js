/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
const {BrowserWindow, app, screen, ipcMain, Menu} = require('electron');
const {dirname, join} = require('path');
const {format} = require('url');
const {spawn} = require('child_process');
const {getModeConfig} = require('./utils');
const isWindows = process.platform === 'win32';

/**
 * Electron main process.
 * Handles Chromium windows and backend process.
 */

// Get .exe path
const exePath = dirname(process.execPath);
// Electron processes
let sdmFrontendWindow = null;
let sdmBackendProcess = null;

/**
 * Bootstraps the application.
 */
const gotTheLock = app.requestSingleInstanceLock();

if (!gotTheLock) {
	app.quit();
} else {
	app.on('second-instance', (event, commandLine, workingDirectory) => {
		// Someone tried to run a second instance, we should focus our window.
		if (sdmFrontendWindow) {
			if (sdmFrontendWindow.isMinimized()) sdmFrontendWindow.restore();
			sdmFrontendWindow.focus();
		}
	});

	app.whenReady().then(() => {
		// Window size & configuration
		const workAreaSize = screen.getPrimaryDisplay().workAreaSize;
		const width = Math.min(1280, workAreaSize.width || 1280);
		const height = Math.min(900, workAreaSize.height || 900);
		sdmFrontendWindow = new BrowserWindow({
			width: width,
			height: height,
			webPreferences: {
				nodeIntegration: true,
				preload: join(__dirname, 'preload.js'),
				enableRemoteModule: true,
			},
			autoHideMenuBar: true,
		});

		// Menu configuration
		const menu = Menu.buildFromTemplate([
			{
				label: 'File',
				submenu: [
					{
						label: 'Toggle developer tools',
						click() {
							sdmFrontendWindow.webContents.toggleDevTools();
						},
						accelerator: 'F12',
					},
					{
						label: 'Exit',
						click() {
							app.quit();
						},
					},
				],
			},
		]);
		sdmFrontendWindow.setMenu(menu);

		// Open settings app
		const modeConfig = getModeConfig('LAUNCHER');

		// Handle app window close event (shutdown backend process)
		sdmFrontendWindow.on('close', function (e) {
			shutdownBackendProcess(e, true);
		});

		loadWindowContent(modeConfig);
	});
}

/**
 * Handles the window content loading.
 */
const loadWindowContent = (modeConfig) => {
	const iconExtension = isWindows ? 'ico' : 'png';
	sdmFrontendWindow.setIcon(join(__dirname, 'assets', `${modeConfig.app}-favicon.${iconExtension}`));

	if (app.isPackaged) {
		// Production
		sdmFrontendWindow.loadURL(
			format({
				pathname: join(__dirname, '..', 'apps', modeConfig.app, 'index.html'),
				protocol: 'file:',
				slashes: true,
			}),
		);
	} else {
		// Development
		sdmFrontendWindow.loadURL(modeConfig.clientUrl);
	}
};

/**
 * Handles SDM backend startup.
 * Called from preload with "startup-sdm-backend" event.
 */
ipcMain.on('startup-sdm-backend', (event, args) => {
	const modeConfig = getModeConfig(args.mode);

	const script = isWindows ? 'backend-server.bat' : 'backend-server.sh';

	sdmBackendProcess = spawn(
		script,
		['startup', args.useWindow ? ' -w' : '', '-p', `profile_${args.profileId}`, args.debug ? '-d' : '', args.debug ? `${modeConfig.debugPort}` : '', '-proxy', '"' + args.proxy + '"'],
		{cwd: exePath + '/resources/', shell: true},
	);

	event.returnValue = {pid: sdmBackendProcess.pid};
});

/**
 * Handles SDM frontend startup.
 * Called from preload with "startup-sdm-frontend" event.
 */
ipcMain.on('startup-sdm-frontend', (event, args) => {
	const modeConfig = getModeConfig(args.mode);

	// Load app in window
	loadWindowContent(modeConfig);

	// Handle app window close event (shutdown backend process)
	sdmFrontendWindow.on('close', function (e) {
		shutdownBackendProcess(e, args.useWindow);
	});

	event.returnValue = `${modeConfig.mode} Frontend`;
});

const shutdownBackendProcess = (e, useWindow) => {
	if (sdmBackendProcess) {
		e.preventDefault();

		const script = isWindows ? 'backend-server.bat' : 'backend-server.sh';

		const shutdownProcess = spawn(
			script,
			['shutdown', useWindow ? ' -w' : '', '-i', `${sdmBackendProcess.pid}`],
			{cwd: exePath + '/resources/', shell: true},
		);

		sdmBackendProcess = null;
		shutdownProcess.on('exit', function () {
			sdmFrontendWindow.close(); // recall window close method.
		});
	}
};
