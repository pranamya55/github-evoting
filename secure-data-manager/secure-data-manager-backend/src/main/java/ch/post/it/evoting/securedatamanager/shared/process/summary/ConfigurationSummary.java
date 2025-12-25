/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static ch.post.it.evoting.evotinglibraries.domain.validations.EncryptionParametersSeedValidation.validateSeed;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateLanguageMap;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateXsToken;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.evotinglibraries.domain.electoralmodel.Language;
import ch.post.it.evoting.securedatamanager.shared.process.summary.preconfigure.PreconfigureSummary;

public class ConfigurationSummary {

	private final String contestId;
	private final ImmutableMap<String, String> contestDescription;
	private final LocalDate contestDate;
	private final String electionEventSeed;
	private final LocalDateTime evotingFromDate;
	private final LocalDateTime evotingToDate;
	private final int gracePeriod;
	private final int voterTotal;
	private final ImmutableList<String> extendedAuthenticationType;
	private final ElectoralBoardSummary electoralBoard;
	private final ImmutableList<ElectionGroupSummary> electionGroups;
	private final ImmutableList<VoteSummary> votes;
	private final ImmutableList<AuthorizationSummary> authorizations;
	private final PreconfigureSummary preconfigureSummary;
	private final String configurationSignature;

	/**
	 * Summary of the contest.
	 *
	 * @param contestId              the contest identifier.
	 * @param contestDescription     the contest description.
	 * @param contestDate            the contest date.
	 * @param electionEventSeed      the election event seed.
	 * @param evotingFromDate        the start date of the contest.
	 * @param evotingToDate          the end date of the contest
	 * @param voterTotal             the total number of voters.
	 * @param electionGroups         the election groups.
	 * @param votes                  the votes.
	 * @param authorizations         the authorizations.
	 * @param configurationSignature the signature of the configuration file.
	 */
	private ConfigurationSummary(final String contestId,
			final ImmutableMap<String, String> contestDescription,
			final LocalDate contestDate,
			final String electionEventSeed,
			final LocalDateTime evotingFromDate,
			final LocalDateTime evotingToDate,
			final int gracePeriod,
			final int voterTotal,
			final ImmutableList<String> extendedAuthenticationType,
			final ElectoralBoardSummary electoralBoard,
			final ImmutableList<ElectionGroupSummary> electionGroups,
			final ImmutableList<VoteSummary> votes,
			final ImmutableList<AuthorizationSummary> authorizations,
			final PreconfigureSummary preconfigureSummary,
			final String configurationSignature) {
		this.contestId = validateXsToken(contestId);
		validateLanguageMap(checkNotNull(contestDescription).entrySet().stream()
				.collect(ImmutableMap.toImmutableMap(entry -> Language.valueOfInsensitive(entry.key()), ImmutableMap.Entry::value)));
		this.contestDescription = contestDescription;
		this.contestDate = checkNotNull(contestDate);
		this.electionEventSeed = validateSeed(electionEventSeed);
		checkNotNull(evotingFromDate);
		checkNotNull(evotingToDate);
		checkArgument(evotingFromDate.isBefore(evotingToDate) || evotingFromDate.equals(evotingToDate),
				"The evotingFromDate must not be after the evotingToDate.");
		this.evotingFromDate = evotingFromDate;
		this.evotingToDate = evotingToDate;
		checkArgument(gracePeriod >= 0, "The grace period must be positive.");
		this.gracePeriod = gracePeriod;
		checkArgument(voterTotal > 0, "There must be at least one voter.");
		this.voterTotal = voterTotal;
		this.extendedAuthenticationType = checkNotNull(extendedAuthenticationType);
		this.electoralBoard = checkNotNull(electoralBoard);
		this.electionGroups = checkNotNull(electionGroups);
		this.votes = checkNotNull(votes);
		this.authorizations = checkNotNull(authorizations);
		this.preconfigureSummary = preconfigureSummary;
		this.configurationSignature = checkNotNull(configurationSignature);
	}

	public String getContestId() {
		return contestId;
	}

	public ImmutableMap<String, String> getContestDescription() {
		return contestDescription;
	}

	public LocalDate getContestDate() {
		return contestDate;
	}

	public String getElectionEventSeed() {
		return electionEventSeed;
	}

	public LocalDateTime getEvotingFromDate() {
		return evotingFromDate;
	}

	public LocalDateTime getEvotingToDate() {
		return evotingToDate;
	}

	public int getGracePeriod() {
		return gracePeriod;
	}

	public int getVoterTotal() {
		return voterTotal;
	}

	public ImmutableList<String> getExtendedAuthenticationType() {
		return extendedAuthenticationType;
	}

	public ElectoralBoardSummary getElectoralBoard() {
		return electoralBoard;
	}

	public ImmutableList<ElectionGroupSummary> getElectionGroups() {
		return electionGroups;
	}

	public ImmutableList<VoteSummary> getVotes() {
		return votes;
	}

	public ImmutableList<AuthorizationSummary> getAuthorizations() {
		return authorizations;
	}

	public PreconfigureSummary getPreconfigureSummary() {
		return preconfigureSummary;
	}

	public String getConfigurationSignature() {
		return configurationSignature;
	}

	public static class Builder {
		private String contestId;
		private ImmutableMap<String, String> contestDescription;
		private LocalDate contestDate;
		private String electionEventSeed;
		private LocalDateTime evotingFromDate;
		private LocalDateTime evotingToDate;
		private int gracePeriod;
		private int voterTotal;
		private ImmutableList<String> extendedAuthenticationType;
		private ElectoralBoardSummary electoralBoard;
		private ImmutableList<ElectionGroupSummary> electionGroups;
		private ImmutableList<VoteSummary> votes;
		private ImmutableList<AuthorizationSummary> authorizations;
		private PreconfigureSummary preconfigureSummary;
		private String configurationSignature;

		public Builder withContestId(final String contestId) {
			this.contestId = contestId;
			return this;
		}

		public Builder withContestDescription(final ImmutableMap<String, String> contest) {
			this.contestDescription = contest;
			return this;
		}

		public Builder withContestDate(final LocalDate contestDate) {
			this.contestDate = contestDate;
			return this;
		}

		public Builder withElectionEventSeed(final String electionEventSeed) {
			this.electionEventSeed = electionEventSeed;
			return this;
		}

		public Builder withEvotingFromDate(final LocalDateTime evotingFromDate) {
			this.evotingFromDate = evotingFromDate;
			return this;
		}

		public Builder withEvotingToDate(final LocalDateTime evotingToDate) {
			this.evotingToDate = evotingToDate;
			return this;
		}

		public Builder withGracePeriod(final int gracePeriod) {
			this.gracePeriod = gracePeriod;
			return this;
		}

		public Builder withVoterTotal(final int voterTotal) {
			this.voterTotal = voterTotal;
			return this;
		}

		public Builder withExtendedAuthenticationType(final ImmutableList<String> extendedAuthenticationType) {
			this.extendedAuthenticationType = extendedAuthenticationType;
			return this;
		}

		public Builder withElectoralBoard(final ElectoralBoardSummary electoralBoard) {
			this.electoralBoard = electoralBoard;
			return this;
		}

		public Builder withElectionGroups(final ImmutableList<ElectionGroupSummary> electionGroups) {
			this.electionGroups = electionGroups;
			return this;
		}

		public Builder withVotes(final ImmutableList<VoteSummary> votes) {
			this.votes = votes;
			return this;
		}

		public Builder withAuthorizations(final ImmutableList<AuthorizationSummary> authorizations) {
			this.authorizations = authorizations;
			return this;
		}

		public Builder withPreconfigureSummary(final PreconfigureSummary preconfigureSummary) {
			this.preconfigureSummary = preconfigureSummary;
			return this;
		}

		public Builder withConfigurationSignature(final String configurationSignature) {
			this.configurationSignature = configurationSignature;
			return this;
		}

		public ConfigurationSummary build() {
			return new ConfigurationSummary(contestId, contestDescription, contestDate, electionEventSeed, evotingFromDate, evotingToDate,
					gracePeriod,
					voterTotal, extendedAuthenticationType, electoralBoard, electionGroups, votes, authorizations, preconfigureSummary,
					configurationSignature);
		}
	}
}
