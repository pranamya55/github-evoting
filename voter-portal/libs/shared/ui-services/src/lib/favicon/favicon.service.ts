/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Inject, Injectable, Renderer2, RendererFactory2} from '@angular/core';
import {DOCUMENT} from '@angular/common';

@Injectable({
	providedIn: 'root',
})
export class FaviconService {
	private readonly renderer: Renderer2;

	constructor(
		private readonly rendererFactory: RendererFactory2,
		@Inject(DOCUMENT) private readonly document: Document,
	) {
		this.renderer = this.rendererFactory.createRenderer(null, null);
	}

	setFavicon(base64: string): void {
		let link: HTMLLinkElement | null =
			this.document.querySelector('link[rel="icon"]');

		// Remove the existing favicon link if it exists
		if (link) {
			this.renderer.removeChild(this.document.head, link);
		}

		// Create a new link element for the favicon
		link = this.renderer.createElement('link');
		this.renderer.setAttribute(link, 'rel', 'icon');
		this.renderer.setAttribute(
			link,
			'href',
			`data:image/x-icon;base64,${base64}`,
		);

		// Append the new favicon link to the document head
		this.renderer.appendChild(this.document.head, link);
	}
}
