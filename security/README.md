# Java Security Configuration

The BFH E-Voting group highlighted the importance of high-quality randomness in
their [report on the Swiss Post E-Voting System](https://www.bk.admin.ch/dam/bk/de/dokumente/pore/Scope%201%20Final%20Report%20BFH%2028.03.2022.pdf.download.pdf/Scope%201%20Final%20Report%20BFH%2028.03.2022.pdf)
.

The Swiss Post Voting System relies on the operating system to select a high-quality PRNG when performing cryptographic operations.
For increased auditability and to ensure that an appropriate PRNG is selected in practice, this folder contains the Java security configuration
for the Linux and Windows operating systems used in the deployed system.