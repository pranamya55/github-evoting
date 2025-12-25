/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {TranslateModule} from '@ngx-translate/core';
import {MockBoardMember, RandomArray, RandomItem,} from '@sdm/shared-util-testing';
import {MockModule} from 'ng-mocks';
import {BoardMembersComponent} from './board-members.component';
import {By} from '@angular/platform-browser';
import {Component, DebugElement} from '@angular/core';
import {BoardMember} from '@sdm/shared-util-types';

@Component({
  standalone: true,
  template: `
    <sdm-board-members
      [boardMembers]="boardMembers"
      [activeMember]="activeMember"
      [passwords]="passwords"
    >
      <form id="mockForm"></form>
      <div id="mockContent"></div>
    </sdm-board-members>
  `,
  imports: [BoardMembersComponent],
})
class TestingHostComponent {
  boardMembers!: BoardMember[];
  activeMember?: BoardMember;
  passwords = new Map();
}

describe('BoardMembersComponent', () => {
  let fixture: ComponentFixture<TestingHostComponent>;
  let host: TestingHostComponent;

  let membersWithPassword: BoardMember[];
  let membersWithoutPassword: BoardMember[];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        BoardMembersComponent,
        TestingHostComponent,
        MockModule(TranslateModule),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TestingHostComponent);
    host = fixture.componentInstance;

    membersWithPassword = RandomArray(MockBoardMember);
    membersWithoutPassword = RandomArray(MockBoardMember);

    host.boardMembers = [...membersWithPassword, ...membersWithoutPassword];
    host.activeMember = RandomItem(host.boardMembers);
    membersWithPassword.forEach((member) => {
      host.passwords.set(member.id, 'Mock Password');
    });

    fixture.detectChanges();
  });

  function removeActiveMember(members: BoardMember[]): BoardMember[] {
    return members.filter((member) => member.id !== host.activeMember?.id);
  }

  function getProvidedForm(): DebugElement {
    return fixture.debugElement.query(By.css('[data-test="form"] #mockForm'));
  }

  function getProvidedContent(): DebugElement {
    return fixture.debugElement.query(
      By.css('[data-test="content"] #mockContent'),
    );
  }

  it('should show all members without a password', () => {
    const displayedMembersWithoutPassword = fixture.debugElement.queryAll(
      By.css('[data-test="membersWithoutPassword"] [data-test="member"]'),
    );

    expect(displayedMembersWithoutPassword.length).toBe(
      removeActiveMember(membersWithoutPassword).length,
    );
  });

  it('should show all members with a password', () => {
    const displayedMembersWithPassword = fixture.debugElement.queryAll(
      By.css('[data-test="membersWithPassword"] [data-test="member"]'),
    );

    expect(displayedMembersWithPassword.length).toBe(
      removeActiveMember(membersWithPassword).length,
    );
  });

  it('should show the name of the active board member', () => {
    const activeMember = fixture.debugElement.query(
      By.css('[data-test="activeMember"]'),
    );

    expect(activeMember).toBeTruthy();
    expect(activeMember.nativeElement.textContent).toContain(
      host.activeMember?.name,
    );
  });

  it('should properly show the provided form', () => {
    expect(getProvidedForm()).toBeTruthy();
  });

  it('should not show the provided form if there is no active board member', () => {
    host.activeMember = undefined;
    fixture.detectChanges();
    expect(getProvidedForm()).toBeFalsy();
  });

  it('should properly show the provided content', () => {
    expect(getProvidedContent()).toBeTruthy();
  });

  it('should not show the provided content if all members have a password', () => {
    host.boardMembers.forEach((member) => {
      host.passwords.set(member.id, 'Mock password');
    });
    fixture.detectChanges();

    expect(getProvidedContent()).toBeFalsy();
  });
});
