/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

export class CompatibilityCheckProcess {

	/**
	 * Checks if the browser is compatible with the voting system.
	 */
	public async isBrowserCompatible(): Promise<boolean> {
		const wasmSupported = this.isWebAssemblySupported();

		if (!wasmSupported) {
			console.warn("Compatibility check: WebAssembly is not supported in this browser, but is required by the voting system.");
			return false;
		} else {
			return true;
		}
	}

	private isWebAssemblySupported(): boolean {
		try {
			if (typeof WebAssembly === 'object' &&
				typeof WebAssembly.instantiate === 'function') {
				// This byte sequence represents the WebAssembly magic number (\0asm) and the version (\1000).
				// It is the smallest valid WebAssembly module.
				const module = new WebAssembly.Module(new Uint8Array([0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00]));
				if (module instanceof WebAssembly.Module)
					return new WebAssembly.Instance(module) instanceof WebAssembly.Instance;
			}
		} catch (_e) {
			return false;
		}
		return false;
	}

}
