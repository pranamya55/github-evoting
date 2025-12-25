/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.cli.command.converter;

import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

import picocli.CommandLine;

public class ComponentConverter implements CommandLine.ITypeConverter<Alias> {
	@Override
	public Alias convert(final String value) {
		checkNotNull(value);

		return Alias.getByComponentName(value);
	}
}
