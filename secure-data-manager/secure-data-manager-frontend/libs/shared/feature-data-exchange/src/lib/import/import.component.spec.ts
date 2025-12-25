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
import {ImportComponent} from './import.component';

describe('ImportComponent', () => {
  let component: ImportComponent;
  let fixture: ComponentFixture<ImportComponent>;

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
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ImportComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
