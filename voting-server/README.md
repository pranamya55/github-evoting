# Voting Server
The voting server is an integral component of the Swiss Post Voting System, facilitating secure and dependable communication between the voting client, online Secure Data Manager, and control components. 
The voting server communicates with the voting client and online Secure Data Manager via HTTPS, and with the control components using JMS. HTTPS (Hypertext Transfer Protocol Secure) is a secure version of HTTP, the primary protocol used for data communication on the World Wide Web, which encrypts data exchange between a client and a server using SSL/TLS encryption to ensure privacy and data integrity. JMS (Java Message Service) is a standard API for message-oriented middleware, designed to enable interoperable and reliable messaging between distributed systems.
This hybrid communication approach guarantees reliable and efficient message exchange within the Swiss Post Voting System.

In addition, the voting server performs authentication of the voting client in the VerifyAuthenticationChallenge algorithm during the execution of the voting phase, making certain that only eligible voters can interact with the voting server.

Furthermore, the voting server executes the ExtractCRC and ExtractVCC algorithms of the cryptographic protocol.

## Usage

The voting server is packaged as a .jar file and deployed in a Spring Boot instance.

## Development

```
mvn clean install
```
