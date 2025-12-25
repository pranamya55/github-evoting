/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ConfigureVoterPortalComponent} from "@sdm/online";

describe('VoterPortalConfigurationComponent', () => {
	let component: ConfigureVoterPortalComponent;
	let fixture: ComponentFixture<ConfigureVoterPortalComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [ConfigureVoterPortalComponent],
		}).compileComponents();

		fixture = TestBed.createComponent(ConfigureVoterPortalComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});
});
