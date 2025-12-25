/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {TranslateModule} from '@ngx-translate/core';
import {MockComponent, MockModule} from 'ng-mocks';
import {PasswordCreationComponent} from '@sdm/shared-feature-passwords';
import {ConstituteElectoralBoardComponent} from './constitute-electoral-board.component';

describe('BoardConstitutionComponent', () => {
	let component: ConstituteElectoralBoardComponent;
	let fixture: ComponentFixture<ConstituteElectoralBoardComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [
				ConstituteElectoralBoardComponent,
				MockModule(TranslateModule),
				MockComponent(PasswordCreationComponent),
			],
		}).compileComponents();

		fixture = TestBed.createComponent(ConstituteElectoralBoardComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});
});
