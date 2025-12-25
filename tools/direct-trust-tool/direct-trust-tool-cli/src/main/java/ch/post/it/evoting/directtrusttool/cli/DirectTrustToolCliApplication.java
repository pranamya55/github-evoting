/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import ch.post.it.evoting.directtrusttool.cli.command.BaseCommand;

import picocli.CommandLine;
import picocli.CommandLine.IFactory;

@SpringBootApplication
public class DirectTrustToolCliApplication implements CommandLineRunner, ExitCodeGenerator {

	private final IFactory factory;
	private final BaseCommand command;
	private int exitCode;

	public DirectTrustToolCliApplication(
			final IFactory factory,
			final BaseCommand command) {
		this.factory = factory;
		this.command = command;
	}

	public static void main(final String[] args) {
		System.exit(SpringApplication.exit(SpringApplication.run(DirectTrustToolCliApplication.class, args)));
	}

	@Override
	public void run(final String... args) {
		exitCode = new CommandLine(command, factory).execute(args);
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}
}
