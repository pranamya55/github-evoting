/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.preconfigure;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = PreConfigureController.class)
public class PreConfigureControllerAdvice {

	private static final Logger LOGGER = LoggerFactory.getLogger(PreConfigureControllerAdvice.class);

	@ExceptionHandler(PreviewSummaryException.class)
	public ResponseEntity<Void> handlePreviewSummaryException(final HttpServletRequest serverHttpRequest,
			final PreviewSummaryException e) {
		LOGGER.error("Failed to process request. [request: {}]", serverHttpRequest.getRequestURI(), e);
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

}
