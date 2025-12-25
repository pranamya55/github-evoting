# Swiss Post Voting System

The Swiss Post Voting System is a return code-based remote online voting system that provides individual verifiability, universal verifiability, and
vote secrecy.

* Individual verifiability: allow a voter to convince herself that the system correctly registered her vote
* Universal verifiability: allow an auditor to check that the election outcome corresponds to the registered votes
* Vote secrecy: do not reveal a voter's vote to anyone

## System Documentation

We provide [extensive documentation](https://gitlab.com/swisspost-evoting/e-voting/e-voting-documentation/-/tree/master)
for the Swiss Post Voting System containing the following documents:

* [Detailed system specification](https://gitlab.com/swisspost-evoting/e-voting/e-voting-documentation/-/tree/master/System)
* [Cryptographic proofs of verifiability and vote secrecy](https://gitlab.com/swisspost-evoting/e-voting/e-voting-documentation/-/tree/master/Protocol)
* [System architecture](https://gitlab.com/swisspost-evoting/e-voting/e-voting-documentation/-/tree/master/System)
* [Infrastructure whitepaper](https://gitlab.com/swisspost-evoting/e-voting/e-voting-documentation/-/tree/master/Operations)

## Changes since 2019

Since the source code's publication in 2019, we improved the source code in the following regards.

* Aligned the source code more faithfully to the system specification.
* Increased the source code's auditability and maintainability.
* Eliminated dead code and redundancies.
* Reduced the overall number of third-party dependencies, updating the remaining in addition to improving framework usage.
* Improved the general code quality.

## Code Quality

To improve code quality, we focus on the following tools:

| Tool                                    | Focus                                                                                              |
|-----------------------------------------|----------------------------------------------------------------------------------------------------|
| [SonarQube](https://www.sonarqube.org/) | Code quality and code security                                                                     |
| [JFrog X-Ray](https://jfrog.com/xray/)  | Common vulnerabilities and exposures (CVE) analysis, Open-source software (OSS) license compliance | |

## Changelog

An overview of all major changes within the published releases is available [here.](CHANGELOG.md)

## Limitations

Regardless of how well-built a software system is, technical debt accrues over time. The architecture documentation (chapter 11) lists technical
debts.

Moreover, no cryptographic protocol is unconditionally secure.
The [cryptographic protocol's documentation](https://gitlab.com/swisspost-evoting/e-voting/e-voting-documentation/-/tree/master/Protocol#limitations)
highlights limitations regarding quantum-resistance, attacks against vote privacy on a malicious voting client, and a trustworthy printing component.

The Swiss Post Voting System's voter portal requires the usage of `wasm-unsafe-eval` in the Content Security Policy (CSP), a security standard introduced 
to prevent Cross-Site Scripting (XSS) and other code injection attacks resulting from execution of malicious content in the trusted web page context.
The `wasm-unsafe-eval` directive in Content Security Policy allows the creation and execution of WebAssembly modules from strings, 
a practice otherwise blocked by default due to security risks associated with dynamic code execution.
However, our cryptographic protocol requires WebAssembly to execute the Argon2id function. 
Argon2id is a state-of-the-art memory-hard hashing algorithm that is not natively available in the Browser's Web Crypto API.

## Build

The [building guide](./BUILDING.md) contains detailed instructions on how to build the e-voting system.

## Reproducible Builds

We provide [reproducible builds](https://reproducible-builds.org/), allowing researchers to verify the path from source code to binaries. We publish the hashes of a Linux-based build.

## Run an end-to-end test

See the repository [evoting-e2e-dev](https://gitlab.com/swisspost-evoting/e-voting/evoting-e2e-dev) for instructions on how to simulate an election
event.

Certain operations run significantly faster using native optimizations. You can check
the [crypto-primitives readme](https://gitlab.com/swisspost-evoting/crypto-primitives/crypto-primitives) for configuring native library support.

In contrast to the productive infrastructure, the development environment omits specific security elements such
as [HTTP security headers](https://owasp.org/www-project-secure-headers/), [DNSSEC](https://www.nic.ch/security/dnssec/)
, [OCSP](https://www.ietf.org/rfc/rfc2560.txt), and [CAA records](https://support.dnsimple.com/articles/caa-record/). You can check the voter portal
README to check our productive configuration of [HTTP security headers](https://owasp.org/www-project-secure-headers/).
