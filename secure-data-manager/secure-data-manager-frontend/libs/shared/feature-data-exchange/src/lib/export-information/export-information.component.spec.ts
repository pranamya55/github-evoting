/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ExportInformationComponent} from './export-information.component';

describe('ExportInformationComponent', () => {
  let component: ExportInformationComponent;
  let fixture: ComponentFixture<ExportInformationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExportInformationComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ExportInformationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
