/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.stream.Stream;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet;

public enum State {

	AARGAU("Aargau"),
	APPENZELL_AUSSERRHODEN("Appenzell Ausserrhoden"),
	APPENZELL_INNERRHODEN("Appenzell Innerrhoden"),
	BASEL_LANDSCHAFT("Basel-Landschaft"),
	BASEL_STADT("Basel-Stadt"),
	BERN("Bern"),
	FRIBOURG("Fribourg"),
	GENEVE("Genève"),
	GLARUS("Glarus"),
	GRISONS("Graubünden"),
	JURA("Jura"),
	LUZERN("Luzern"),
	NEUCHATEL("Neuchâtel"),
	NIDWALDEN("Nidwalden"),
	OBWALDEN("Obwalden"),
	ST_GALLEN("St.Gallen"),
	SCHAFFHAUSEN("Schaffhausen"),
	SCHWYZ("Schwyz"),
	SOLOTHURN("Solothurn"),
	THURGAU("Thurgau"),
	TICINO("Ticino"),
	URI("Uri"),
	VALAIS("Valais"),
	VAUD("Vaud"),
	ZUG("Zug"),
	ZURICH("Zürich");

	private static final ImmutableMap<String, State> STATE_MAP = Stream.of(State.values())
			.collect(toImmutableMap(state -> state.label, state -> state));

	private final String label;

	State(final String label) {
		this.label = label;
	}

	public static ImmutableSet<String> getLabels() {
		return STATE_MAP.keySet();
	}

	public static void isValidLabel(final String label) {
		checkNotNull(label);
		checkArgument(STATE_MAP.containsKey(label), String.format("State '%s' does not exist.", label));
	}

	public String getLabel() {
		return label;
	}
}
