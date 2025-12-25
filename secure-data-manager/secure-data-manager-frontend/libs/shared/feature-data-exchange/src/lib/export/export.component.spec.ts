/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';
import {ActivatedRoute} from '@angular/router';
import {TranslateModule} from '@ngx-translate/core';
import {ToastService} from '@sdm/shared-ui-services';
import {MockProvider} from 'ng-mocks';
import {of} from 'rxjs';
import {DataExchangeService} from '../data-exchange.service';
import {ExportComponent} from './export.component';

describe('ExportComponent', () => {
  let component: ExportComponent;
  let fixture: ComponentFixture<ExportComponent>;

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
        MockProvider(DataExchangeService),
        MockProvider(ToastService),
      ],
    });
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ExportComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
