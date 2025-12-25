/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

public record ElectoralBoard(String id, String alias, ImmutableList<BoardMember> members) {
}
