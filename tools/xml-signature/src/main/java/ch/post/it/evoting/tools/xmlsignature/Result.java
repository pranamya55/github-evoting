/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.xmlsignature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonPropertyOrder({ "signatureVerified", "exceptionMessage" })
public record Result(boolean signatureVerified, String exceptionMessage) {

	private static final Logger LOGGER_CONSOLE = LoggerFactory.getLogger("console");

	public void logAsJson() {
		try {
			final String s = new ObjectMapper().writeValueAsString(new Result(signatureVerified, exceptionMessage));
			if (exceptionMessage != null) {
				LOGGER_CONSOLE.error(s);
			} else {
				LOGGER_CONSOLE.info(s);
			}
		} catch (final JsonProcessingException e) {
			final ObjectNode objectNode = new ObjectMapper().createObjectNode();
			objectNode.put("signatureVerified", signatureVerified);
			objectNode.put("exceptionMessage", e.getMessage());
			LOGGER_CONSOLE.error(objectNode.toString());
		}
	}
}