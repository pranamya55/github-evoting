/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {BallotBoxListComponent} from './ballot-box-list.component';

describe('BallotBoxListComponent', () => {
  let component: BallotBoxListComponent;
  let fixture: ComponentFixture<BallotBoxListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BallotBoxListComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(BallotBoxListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
