/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MockComponents} from 'ng-mocks';
import {StepperItemComponent} from '../stepper-item/stepper-item.component';
import {StepperComponent} from './stepper.component';
import {APP_ROUTES} from "@sdm/shared-util-types";

describe('StepperComponent', () => {
  let component: StepperComponent;
  let fixture: ComponentFixture<StepperComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StepperComponent, MockComponents(StepperItemComponent)],
      providers: [
        {
          provide: APP_ROUTES,
          useValue: [],
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StepperComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
