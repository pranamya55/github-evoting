/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.tally.mixdecrypt;

record BallotBoxPrimaryKey(String electionEventId, String ballotBoxId, int nodeId) {}
