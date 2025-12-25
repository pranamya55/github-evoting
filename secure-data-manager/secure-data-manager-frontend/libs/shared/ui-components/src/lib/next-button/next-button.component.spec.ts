/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {NextButtonComponent} from './next-button.component';
import {MockDirective, MockModule, MockProvider} from "ng-mocks";
import {RoutingService} from "@sdm/shared-ui-services";
import {TranslateModule} from "@ngx-translate/core";
import {RouterLink} from "@angular/router";

describe('NextButtonComponent', () => {
	let component: NextButtonComponent;
	let fixture: ComponentFixture<NextButtonComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [NextButtonComponent, MockModule(TranslateModule), MockDirective(RouterLink)],
			providers: [MockProvider(RoutingService)]
		}).compileComponents();

		fixture = TestBed.createComponent(NextButtonComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});
});
