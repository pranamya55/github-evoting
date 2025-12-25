/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.metrics;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;

@RestController
@RequestMapping("/sdm-shared/metrics")
public class MetricsController {
	private static final Logger LOGGER = LoggerFactory.getLogger(MetricsController.class);

	@GetMapping(produces = "application/json", value = "processorName")
	public ResponseEntity<Object> getProcessorName() {
		try {
			final String processorName = System.getProperty("os.arch");
			if (!processorName.isEmpty()) {
				return ResponseEntity.ok(ImmutableMap.of("name", processorName));
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
			}
		} catch (final IllegalArgumentException e) {
			LOGGER.error("Error trying to get processor name.", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}
	}

}
