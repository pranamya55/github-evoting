/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
const path = require('path');

module.exports = {
	mode: 'production',
	entry: {
		api: './src/api.ts',
		worker: './src/worker-api.ts',
	},
	output: {
		path: path.resolve(__dirname, 'dist'),
		filename: 'ov-[name].js',
	},
	module: {
		rules: [
			{
				test: /\.ts?$/,
				use: 'ts-loader',
				exclude: /node_modules/,
            },
		],
	},
	resolve: {
		fallback: {crypto: false},
		extensions: ['.ts', '.js']
	},
	performance: {
		maxAssetSize: 1500000,
		maxEntrypointSize: 1500000,
	},
	optimization: {
		minimize: false,
		usedExports: true,
	}
};