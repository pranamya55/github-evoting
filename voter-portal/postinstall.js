/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
const { execSync } = require('child_process');
const os = require('os');

function installDependency() {
	const platform = os.platform();
	const arch = os.arch();

	let dependency;

	if (platform === 'darwin' && arch === 'arm64') {
		dependency = '@nx/nx-darwin-arm64';
	} else if (platform === 'linux' && arch === 'x64') {
		dependency = '@nx/nx-linux-x64-gnu';
	} else if (platform === 'win32' && arch === 'x64') {
		dependency = '@nx/nx-win32-x64-msvc';
	} else {
		console.error(`Unsupported platform: ${platform} ${arch}`);
		process.exit(1);
	}

	console.log(`Installing ${dependency}...`);
	execSync(`npm install ${dependency}`, { stdio: 'inherit' });
}

installDependency();
