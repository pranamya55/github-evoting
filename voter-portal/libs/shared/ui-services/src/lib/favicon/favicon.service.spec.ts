/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {TestBed} from '@angular/core/testing';
import {FaviconService} from './favicon.service';
import {provideMockStore} from '@ngrx/store/testing';

describe('FaviconService', () => {
	let service: FaviconService;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: provideMockStore({}),
		});
		service = TestBed.inject(FaviconService);
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});

	it('replaces existing favicon link with new base64 favicon', () => {
		const mockLink = document.createElement('link');
		mockLink.setAttribute('rel', 'icon');
		document.head.appendChild(mockLink);

		service.setFavicon('dGVzdA==');
		const newLink = document.head.querySelector('link[rel="icon"]');
		expect(newLink).toBeTruthy();
		expect(newLink?.getAttribute('href')).toBe('data:image/x-icon;base64,dGVzdA==');
	});

	it('adds favicon link if none exists', () => {
		const existing = document.head.querySelector('link[rel="icon"]');
		if (existing) {
			document.head.removeChild(existing);
		}
		service.setFavicon('YmFzZTY0');
		const link = document.head.querySelector('link[rel="icon"]');
		expect(link).toBeTruthy();
		expect(link?.getAttribute('href')).toBe('data:image/x-icon;base64,YmFzZTY0');
	});

	it('handles multiple calls by always keeping only one favicon link', () => {
		service.setFavicon('Zmlyc3Q=');
		service.setFavicon('c2Vjb25k');
		const links = document.head.querySelectorAll('link[rel="icon"]');
		expect(links.length).toBe(1);
		expect(links[0].getAttribute('href')).toBe('data:image/x-icon;base64,c2Vjb25k');
	});
});
