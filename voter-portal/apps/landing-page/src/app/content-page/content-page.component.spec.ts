/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ContentPageComponent} from './content-page.component';
import {TenantConfigurationService} from '@vp/landing-page-data-access';
import {of} from 'rxjs';
import {MockComponent, MockProvider} from "ng-mocks";
import {ContentElementComponent, ModeElementComponent} from "@vp/landing-page-ui-components";
import {LangChangeEvent, TranslateService} from "@ngx-translate/core";
import {By, Title} from "@angular/platform-browser";
import {HeadingContentElement} from "@vp/landing-page-utils-types";
import {RandomArray, RandomKey} from "@vp/shared-util-testing";
import {EventEmitter} from "@angular/core";

describe('ContentPageComponent', () => {
	let fixture: ComponentFixture<ContentPageComponent>;
	let mockConfiguration: { heading: HeadingContentElement }[];
	let mockLangChange: EventEmitter<LangChangeEvent>;

	beforeEach(() => {
		mockLangChange = new EventEmitter<LangChangeEvent>;
		mockConfiguration = RandomArray(i => ({
			heading: {
				level: i === 0 ? 1 : 2,
				text: {
					DE: `text-de-${i}`,
					FR: `text-fr-${i}`,
					IT: `text-it-${i}`,
					RM: `text-rm-${i}`,
					EN: `text-en-${i}`,
				}
			}
		}))

		TestBed.configureTestingModule({
			imports: [
				ContentPageComponent,
				MockComponent(ModeElementComponent),
				MockComponent(ContentElementComponent)
			],
			providers: [
				MockProvider(TenantConfigurationService, {
					getContentConfiguration: jest.fn().mockReturnValue(of(mockConfiguration)),
				}),
				MockProvider(TranslateService, {
					onLangChange: mockLangChange
				}),
				MockProvider(Title, {
					setTitle: jest.fn(),
				}),
			]
		});

		fixture = TestBed.createComponent(ContentPageComponent);

		fixture.detectChanges();
	});

	it('should show all elements from the config', () => {
		const displayedElements = fixture.debugElement.queryAll(By.directive(ContentElementComponent));
		expect(displayedElements.length).toEqual(mockConfiguration.length);
	});

	it('should set the browser tab title depending on the content', () => {
		const mockPageTitle = 'mock page title';
		const mainHeader = mockConfiguration[0];
		const randomLang = RandomKey(mainHeader.heading.text);

		const titleService = fixture.debugElement.injector.get(Title);
		mockLangChange.emit({lang: randomLang.toLowerCase(), translations: {'common.pageTitle': mockPageTitle}});

		expect(titleService.setTitle).toHaveBeenCalledWith(`${mockPageTitle} - ${mainHeader.heading.text[randomLang]}`);
	});
});