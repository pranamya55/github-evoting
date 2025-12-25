/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import {
	NgbAccordionModule,
	NgbActiveModal,
	NgbModal,
} from '@ng-bootstrap/ng-bootstrap';
import { TranslationListDirective } from '@vp/voter-portal-ui-directives';
import { ConfigurationService } from '@vp/voter-portal-ui-services';
import { RandomElectionEventId, RandomKey } from '@vp/shared-util-testing';
import { ExtendedFactor, FAQSection, VoterPortalConfig } from '@vp/voter-portal-util-types';
import { MockDirective, MockPipe, MockProvider } from 'ng-mocks';
import { TranslateTestingModule } from 'ngx-translate-testing';

import { FAQModalComponent } from '@vp/voter-portal-feature-faq';
import { MarkdownPipe } from 'e-voting-libraries-ui-kit';

describe('FAQModalComponent', () => {
	let component: FAQModalComponent;
	let fixture: ComponentFixture<FAQModalComponent>;
	let dismiss: jest.Mock<() => void>;
	let voterPortalConfig: VoterPortalConfig;

	beforeEach(async () => {
		dismiss = jest.fn();
		voterPortalConfig = {
			identification: ExtendedFactor.YearOfBirth,
			contestsCapabilities: {
				writeIns: true,
			},
			requestTimeout: {
				authenticateVoter: 30000,
				sendVote: 120000,
				confirmVote: 120000,
			},
			header: {
				logo: '',
				logoHeight: { desktop: 0, mobile: 0 },
			},
			electionEventId: RandomElectionEventId(),
			favicon: '',
		};

		await TestBed.configureTestingModule({
			declarations: [
				FAQModalComponent,
				MockDirective(TranslationListDirective),
				MockPipe(MarkdownPipe),
			],
			imports: [
				TranslateTestingModule.withTranslations({}),
				NgbAccordionModule,
			],
			providers: [
				MockProvider(NgbModal),
				MockProvider(NgbActiveModal, { dismiss }),
				MockProvider(ConfigurationService, voterPortalConfig),
			],
		}).compileComponents();
	});

	describe('without an active section', () => {
		beforeEach(() => {
			fixture = TestBed.createComponent(FAQModalComponent);
			component = fixture.componentInstance;

			fixture.detectChanges();
		});

		it('should show all the FAQ', () => {
			const faqTitles = fixture.debugElement.queryAll(
				By.css('.accordion-item'),
			);
			expect(faqTitles.length).toBe(12);
		});

		it('should not show any accordion content', () => {
			const accordionContents = fixture.debugElement.queryAll(
				By.css('.accordion-collapse.show'),
			);
			expect(accordionContents.length).toBe(0);
		});
	});

	describe('with an active section', () => {
		let activeSection: FAQSection;

		beforeEach(() => {
			fixture = TestBed.createComponent(FAQModalComponent);
			component = fixture.componentInstance;

			activeSection = component.activeFAQSection =
				FAQSection[RandomKey(FAQSection)];

			fixture.detectChanges();
		});

		it('should show only accordion content matching the active section provided', () => {
			const accordionContents = fixture.debugElement.queryAll(
				By.css('.accordion-collapse.show'),
			);
			expect(accordionContents.length).toBe(1);
			expect(accordionContents[0].nativeElement.firstChild.id).toBe(
				`${activeSection}-content`,
			);
		});

		it("should focus on the active section's accordion header button", () => {
			const activeSectionButton: HTMLElement = fixture.debugElement.query(
				By.css(`#${activeSection}-header > button`),
			).nativeElement;
			jest.spyOn(activeSectionButton, 'focus');

			component.ngAfterViewInit();

			expect(activeSectionButton.focus).toHaveBeenCalled();
		});
	});

	describe('with "How do I start?" section active', () => {
		beforeEach(() => {
			fixture = TestBed.createComponent(FAQModalComponent);
			component = fixture.componentInstance;

			component.activeFAQSection = FAQSection.HowToStart;

			fixture.detectChanges();
		});

		it('should properly adapt to the portal configuration', () => {
			const howtoStartSection: HTMLElement = fixture.debugElement.query(
				By.css(`#${FAQSection.HowToStart}-content`),
			).nativeElement;

			expect(howtoStartSection.textContent).toContain(
				'faq.howtostart.content.' + voterPortalConfig.identification,
			);
		});
	});
});
