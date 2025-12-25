/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	@Value("${mvcTaskExecutor.poolSize}")
	private int taskExecutorPoolSize;

	@Value("${mvcTaskExecutor.timeout}")
	private long taskExecutorTimeout;

	@Bean
	public ThreadPoolTaskExecutor mvcTaskExecutor() {
		final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(taskExecutorPoolSize);
		taskExecutor.setMaxPoolSize(taskExecutorPoolSize);
		return taskExecutor;
	}

	@Override
	public void configureAsyncSupport(final AsyncSupportConfigurer configurer) {
		configurer.setTaskExecutor(mvcTaskExecutor());
		configurer.setDefaultTimeout(taskExecutorTimeout);
	}

}
