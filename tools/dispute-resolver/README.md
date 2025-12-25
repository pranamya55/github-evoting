# Dispute Resolver

The Dispute Resolver is a Spring Boot application executed via a command-line interface designed to resolve a dispute between the Control Components.
It reads signed payloads from the Control Components, resolves the dispute and writes the result in a signed payload.

The Dispute Resolver is only executed in rare and exceptional cases where the Control Components do not reach consensus on the list of confirmed
votes.
The system specification elaborates on this scenario in the section "Handling Inconsistent Views of Confirmed Votes".

It is assumed that the Dispute Resolver operates in a controlled, offline environment, with appropriate organizational and operational procedures in
place to ensure its trustworthy and secure execution.

## Usage

The following parameters must be provided:

* `input.directory`: Path to the directory containing input payload files. For each node id, the directory must contain the
  `controlComponentExtractedElectionEventPayload.<nodeId>.json` and `controlComponentExtractedVerificationCardsPayload.<nodeId>.json`.
* `output.directory`: Path to the directory where output payload files will be written.
* `direct-trust.keystore.location`: Path to the keystore file of the Dispute Resolver.
* `direct-trust.password.location`: Path to the keystore password file of the Dispute Resolver.

Run the application:

```bash
java -Dinput.directory=<input-directory> \
     -Doutput.directory=<output-directory> \
     -Ddirect-trust.keystore.location=<keystore-path> \
     -Ddirect-trust.password.location=<password-path> \
     -jar dispute-resolver-<VERSION>-runnable.jar
```

## Development

```bash
mvn clean install
```

## Test Data

Test data is available in the [testdata repository](https://gitlab.com/swisspost-evoting/e-voting/testdata).

## Additional Documentation

| Name                 | Link                                                                                                                                                      |
|----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| System Specification | [System Specification](https://gitlab.com/swisspost-evoting/e-voting/e-voting-documentation/-/blob/master/System/System_Specification.pdf?ref_type=heads) |
| Test Data            | [Testdata Repository](https://gitlab.com/swisspost-evoting/e-voting/testdata)                                                                             |
