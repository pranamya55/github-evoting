/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {PageActionsComponent} from './page-actions.component';
import {MockProvider} from "ng-mocks";
import {VotingServerHealthService, WorkflowStateService} from "@sdm/shared-ui-services";
import {ActivatedRoute} from "@angular/router";

describe('PageActionsComponent', () => {
  let component: PageActionsComponent;
  let fixture: ComponentFixture<PageActionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [
        MockProvider(WorkflowStateService),
        MockProvider(ActivatedRoute, {
          snapshot: {
            data: {}
          }
        } as any),
        MockProvider(VotingServerHealthService)
      ],
      imports: [PageActionsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PageActionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
