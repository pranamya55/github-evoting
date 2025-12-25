/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;

@SpringBootApplication
public class DirectTrustToolBackendApplication {

	public static void main(final String[] args) {
		SpringApplication.run(DirectTrustToolBackendApplication.class, args);
	}

	@Bean
	public ObjectMapper evotingMapper(){
		return DomainObjectMapper.getNewInstance();
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(final CorsRegistry registry) {
				registry.addMapping("/**").allowedMethods("*").allowedHeaders("*");
			}
		};
	}
}
