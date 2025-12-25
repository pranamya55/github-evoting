# XML Signature Tool

The XML Signature Tool is a digital signature tool that creates and verifies digital signatures for XML files. Digital signatures play a critical role
in ensuring the authenticity and integrity of files, preventing impersonation attacks, and providing non-repudiation of origin.

The XML Signature Tool utilizes the "direct trust" approach for channel security, a mechanism that distributes the certificates via an out-of-band
channel. This approach provides a robust way for authentication and non-repudiation, ensuring the authenticity of the files being signed. The Direct
Trust approach for channel security is described in
the [system specifications](https://gitlab.com/swisspost-evoting/e-voting/e-voting-documentation/-/blob/master/System/System_Specification.pdf) and
the [architecture document](https://gitlab.com/swisspost-evoting/e-voting/e-voting-documentation/-/blob/master/System/SwissPost_Voting_System_architecture_document.pdf),
providing a clear understanding of how the tool operates and the security measures it employs.

The XML Signature Tool can be embedded as a JAR file in third-party applications that wish to integrate with the direct trust approach. The XML
Signature Tool is essential for applications requiring a secure and reliable method of signing and verifying XML files.
The use of the Direct Trust approach and the ability to embed the tool into third-party applications makes it a versatile and powerful tool for
ensuring the authenticity and integrity of files in the e-voting domain.

XML (eXtensible Markup Language) files are widely used as an interface between different systems due to their ability to facilitate easy content
validation. The open eCH-standard, widely used in Switzerland for data exchange in the e-government domain, also utilizes XML files as its primary
format for data representation.

## Usage

The XML signature tool is a standalone tool executed via a command-line interface.

```bash
java -Ddirect-trust.keystore.location=<direct-trust-keystoreFile> -Ddirect-trust.password.location=<direct-trust-passFile> -jar xml-signature-<VERSION>-runnable.jar <CONFIG|PRINT> <SIGN|VERIFY> <filePath>
```

The following parameters must be provided:

* The location of the direct trust keystore
* The password file for the direct trust keystore
* The xml-signature jar
* The path of the XML file to sign or to verify

## Development

```bash
mvn clean install
```
