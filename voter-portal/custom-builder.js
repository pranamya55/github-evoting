/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
const { executeBrowserBuilder } = require('@angular-devkit/build-angular');
const { createBuilder } = require('@angular-devkit/architect');
const { Compilation, sources } = require('webpack');

class GoogMsgVarUnificationPlugin {
	apply(compiler) {
		compiler.hooks.thisCompilation.tap(
			GoogMsgVarUnificationPlugin.name,
			(compilation) => {
				compilation.hooks.processAssets.tap(
					{
						name: GoogMsgVarUnificationPlugin.name,
						stage: Compilation.PROCESS_ASSETS_STAGE_OPTIMIZE,
					},
					() => {
						const fileName = 'main.js';
						const file = compilation.getAsset(fileName);

						if (file) {
							const template = file.source.source();
							const updatedTemplate = template.replace(
								/MSG_([A-Za-z0-9_]+)NODE_MODULES_([A-Za-z0-9_]+)/g,
								(_, __, varName) => `MSG_NODE_MODULES_${varName}`
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
				plugins: [...config.plugins, new GoogMsgVarUnificationPlugin()],
			}),
	});
}

exports.default = createBuilder(buildCustomWebpackBrowser);
