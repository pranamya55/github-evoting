/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
export function focusFirstInvalidControl() {
	setTimeout(() => {
		const firstInvalid = document.querySelector<HTMLInputElement>(
			':where(input, textarea).is-invalid',
		);

		if (!firstInvalid) return;

		const elementToShow = firstInvalid.closest('vp-answer') ?? firstInvalid;

		const offsetToShow = elementToShow.getBoundingClientRect().top;
		const isAlreadyVisible = offsetToShow >= window.scrollY;

		if (!isAlreadyVisible) {
			const header = document.querySelector('vp-header');
			const offsetToScrollBy = offsetToShow - (header ? header.clientHeight : 0) - 20;

			window.scrollBy({top: offsetToScrollBy, behavior: 'smooth'});
		}

		firstInvalid.focus();

		if (firstInvalid.hasAttribute('mask')) {
			firstInvalid.click();
		}
	});
}