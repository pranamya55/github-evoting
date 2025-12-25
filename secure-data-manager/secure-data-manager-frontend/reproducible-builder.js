/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
const {executeBrowserBuilder} = require('@angular-devkit/build-angular');
const {createBuilder} = require('@angular-devkit/architect');
const {Compilation, sources} = require('webpack');

class VarUnificationPlugin {
	apply(compiler) {
		compiler.hooks.thisCompilation.tap(
			VarUnificationPlugin.name,
			(compilation) => {
				compilation.hooks.processAssets.tap(
					{
						name: VarUnificationPlugin.name,
						stage: Compilation.PROCESS_ASSETS_STAGE_OPTIMIZE,
					},
					() => {
						const fileName = 'main.js';
						const file = compilation.getAsset(fileName);

						if (file) {
							const template = file.source.source();
							let updatedTemplate = template.replace(
								/MSG_([A-Za-z0-9_]+)NODE_MODULES_([A-Za-z0-9_]+)/g,
								(_, __, varName) => `MSG_NODE_MODULES_${varName}`
							);

							updatedTemplate = updatedTemplate.replace(
								/([A-Za-z0-9_]+)_node_modules_([A-Za-z0-9_]+)(__WEBPACK_IMPORTED_MODULE[A-Za-z0-9_]+)/g,
								(_, __, varName) => `__import_node_modules__${varName}`
							);

							compilation.updateAsset(
								fileName,
								new sources.RawSource(updatedTemplate)
							);
						}
					}
				);
			}
		);
	}
}

function buildCustomWebpackBrowser(options, context) {
	return executeBrowserBuilder(options, context, {
		webpackConfiguration: (config) =>
			Promise.resolve({
				...config,
				plugins: [...config.plugins, new VarUnificationPlugin()],
			}),
	});
}

exports.default = createBuilder(buildCustomWebpackBrowser);
