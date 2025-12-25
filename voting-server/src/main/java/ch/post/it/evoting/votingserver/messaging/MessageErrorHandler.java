/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ErrorHandler;

@Service
public class MessageErrorHandler implements ErrorHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageErrorHandler.class);

	@Override
	public void handleError(final Throwable throwable) {
		LOGGER.error("Unable to consume message: {}", throwable.getCause().getMessage(), throwable);
	}

}
