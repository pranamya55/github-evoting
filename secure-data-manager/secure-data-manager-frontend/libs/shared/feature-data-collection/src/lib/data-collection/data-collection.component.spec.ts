/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';
import {MockProvider} from 'ng-mocks';
import {ActivatedRoute} from '@angular/router';
import {of} from 'rxjs';
import {TranslateModule} from '@ngx-translate/core';
import {LoadingService, ToastService} from '@sdm/shared-ui-services';
import {DataCollectionService} from "./data-collection.service";
import {DataCollectionComponent} from "./data-collection.component";

describe('ExportComponent', () => {
	let component: DataCollectionComponent;
	let fixture: ComponentFixture<DataCollectionComponent>;

	beforeEach(waitForAsync(() => {
		TestBed.configureTestingModule({
			imports: [TranslateModule.forRoot()],
			providers: [
				MockProvider(ActivatedRoute, {
					data: of({
						exchangeIndex: 'testExchangeIndex',
						nextRoute: 'testNextRoute',
					}),
				}),
				MockProvider(DataCollectionService),
				MockProvider(ToastService),
				MockProvider(LoadingService),
			],
		});
	}));

	beforeEach(() => {
		fixture = TestBed.createComponent(DataCollectionComponent);
		component = fixture.componentInstance;
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});
});
