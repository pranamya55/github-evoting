/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ProcessCancellationService} from '@vp/voter-portal-ui-services';
import {IconComponent} from '@vp/shared-ui-components';
import {MockComponent, MockPipe, MockProvider} from 'ng-mocks';
import {FooterComponentComponent} from './footer.component';
import {By} from '@angular/platform-browser';
import {Component, DebugElement} from '@angular/core';
import {TranslatePipe} from '@ngx-translate/core';
import {CancelMode, FooterCancelMode} from "@vp/voter-portal-util-types";

@Component({
	standalone: false,
	template: `
		<vp-footer [cancelMode]="footerCancelMode">
			<button type="button" id="host-button">Click me</button>
		</vp-footer>
	`,
})
class TestingHostComponent {
	footerCancelMode!: FooterCancelMode;
	protected readonly CancelMode = CancelMode;
}

describe('FooterComponentComponent', () => {
	let component: TestingHostComponent;
	let fixture: ComponentFixture<TestingHostComponent>;
	let mockProcessCancellationService: jest.Mocked<ProcessCancellationService>;
	let cancelButton: DebugElement;
	let hostButton: DebugElement;

	beforeEach(async () => {
		mockProcessCancellationService = {
			cancelVote: jest.fn(),
			leaveProcess: jest.fn(),
		} as any;

		await TestBed.configureTestingModule({
			imports: [MockComponent(IconComponent)],
			declarations: [
				TestingHostComponent,
				FooterComponentComponent,
				MockPipe(TranslatePipe),
			],
			providers: [
				MockProvider(
					ProcessCancellationService,
					mockProcessCancellationService,
				),
			],
		}).compileComponents();

		fixture = TestBed.createComponent(TestingHostComponent);
		component = fixture.componentInstance;
	});

	function setFooterCancelMode(cancelMode: FooterCancelMode) {
		component.footerCancelMode = cancelMode;

		fixture.detectChanges();

		cancelButton = fixture.debugElement.query(By.css('#cancel-process'));
		hostButton = fixture.debugElement.query(By.css('#host-button'));
	}

	describe('mode is CancelVote', () => {
		beforeEach(() => {
			setFooterCancelMode(CancelMode.CancelVote);
		});

		it('should display the host content as first element', () => {
			expect(hostButton).toBeTruthy();
			expect(hostButton.nativeElement.matches(':first-child')).toBeTruthy();
		});

		it('should display the cancel button as last element', () => {
			expect(cancelButton).toBeTruthy();
			expect(cancelButton.nativeElement.matches(':last-child')).toBeTruthy();
		});

		it('should call cancelVote when the cancel button is clicked', () => {
			cancelButton.nativeElement.click();
			expect(mockProcessCancellationService.cancelVote).toHaveBeenCalled();
		});
	});

	describe('mode is LeaveProcess', () => {
		beforeEach(() => {
			setFooterCancelMode(CancelMode.LeaveProcess);
		});

		it('should display the host content as first element', () => {
			expect(hostButton).toBeTruthy();
			expect(hostButton.nativeElement.matches(':first-child')).toBeTruthy();
		});

		it('should display the cancel button as last element', () => {
			expect(cancelButton).toBeTruthy();
			expect(cancelButton.nativeElement.matches(':last-child')).toBeTruthy();
		});

		it('should call leaveProcess when the cancel button is clicked', () => {
			cancelButton.nativeElement.click();
			expect(mockProcessCancellationService.leaveProcess).toHaveBeenCalled();
		});
	});

	describe('mode is false', () => {
		beforeEach(() => {
			setFooterCancelMode(false);
		});

		it('should only display the host content', () => {
			expect(hostButton).toBeTruthy();
			expect(hostButton.nativeElement.matches(':only-child')).toBeTruthy();
		});
	});
});
