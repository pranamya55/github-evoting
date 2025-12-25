/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {focusFirstInvalidControl} from '../focus-first-invalid-control';

describe('focusFirstInvalidControl', () => {
	beforeEach(() => {
		document.body.innerHTML = '';
		jest.useFakeTimers();
	});

	it('should do nothing if no invalid input exists', () => {
		focusFirstInvalidControl();
		jest.runAllTimers();
		// No errors, nothing to focus, so just ensure no exceptions
	});

	it('should focus the first invalid input without mask', () => {
		const input = document.createElement('input');
		input.classList.add('is-invalid');
		document.body.appendChild(input);

		const focusSpy = jest.spyOn(input, 'focus');

		focusFirstInvalidControl();
		jest.runAllTimers();

		expect(focusSpy).toHaveBeenCalled();
	});

	it('should click the first invalid input with mask attribute', () => {
		const input = document.createElement('input');
		input.classList.add('is-invalid');
		input.setAttribute('mask', 'AA-000-AA');
		document.body.appendChild(input);

		const clickSpy = jest.spyOn(input, 'click');

		focusFirstInvalidControl();
		jest.runAllTimers();

		expect(clickSpy).toHaveBeenCalled();
	});

	it('should only act on the first invalid element', () => {
		const first = document.createElement('input');
		first.classList.add('is-invalid');
		document.body.appendChild(first);

		const second = document.createElement('input');
		second.classList.add('is-invalid');
		document.body.appendChild(second);

		const firstFocusSpy = jest.spyOn(first, 'focus');
		const secondFocusSpy = jest.spyOn(second, 'focus');

		focusFirstInvalidControl();
		jest.runAllTimers();

		expect(firstFocusSpy).toHaveBeenCalled();
		expect(secondFocusSpy).not.toHaveBeenCalled();
	});

	it('should work with textarea elements as well', () => {
		const textarea = document.createElement('textarea');
		textarea.classList.add('is-invalid');
		document.body.appendChild(textarea);

		const focusSpy = jest.spyOn(textarea, 'focus');

		focusFirstInvalidControl();
		jest.runAllTimers();

		expect(focusSpy).toHaveBeenCalled();
	});
});