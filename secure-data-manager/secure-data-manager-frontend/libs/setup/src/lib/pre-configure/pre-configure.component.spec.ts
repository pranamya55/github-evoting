/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {TranslateModule} from '@ngx-translate/core';
import {MockComponent, MockDirective, MockModule, MockProvider} from 'ng-mocks';
import {HttpClientModule} from "@angular/common/http";
import {NextButtonComponent} from "@sdm/shared-ui-components";
import {RouterLinkPreviousDirective} from "@sdm/shared-ui-directives";
import {NgxMaskDirective} from "ngx-mask";
import {PreConfigureComponent} from './pre-configure.component';
import {PreConfigureService} from './pre-configure.service';

describe('PreConfigurationComponent', () => {
	let component: PreConfigureComponent;
	let fixture: ComponentFixture<PreConfigureComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [PreConfigureComponent],
			imports: [
				MockModule(ReactiveFormsModule),
				MockComponent(NextButtonComponent),
				MockDirective(RouterLinkPreviousDirective),
				MockModule(TranslateModule),
				MockModule(HttpClientModule),
				MockDirective(NgxMaskDirective)
			],
			providers: [
				MockProvider(PreConfigureService),
			],
		}).compileComponents();

		fixture = TestBed.createComponent(PreConfigureComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();

	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});
});
