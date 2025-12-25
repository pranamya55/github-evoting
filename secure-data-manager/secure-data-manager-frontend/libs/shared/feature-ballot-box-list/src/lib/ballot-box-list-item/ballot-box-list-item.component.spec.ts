/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {BallotBoxListItemComponent} from './ballot-box-list-item.component';

describe('BallotBoxListItemComponent', () => {
  let component: BallotBoxListItemComponent;
  let fixture: ComponentFixture<BallotBoxListItemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BallotBoxListItemComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(BallotBoxListItemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
