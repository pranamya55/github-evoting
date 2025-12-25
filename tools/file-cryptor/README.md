# File Cryptor Tool

The File Cryptor Tool is a versatile encryption and decryption tool designed to secure any file using a password. Encryption is vital for protecting
sensitive information from unauthorized access, ensuring privacy, and maintaining data integrity. By encrypting files, the File Cryptor Tool
safeguards against data breaches and unauthorized viewing, providing a layer of security for personal or professional data.

## Usage

The File Cryptor Tool is a simple Spring Boot application executed via a command-line interface. The following parameters must be provided:

* The mode of the tool. It can be either `ENCRYPT` or `DECRYPT`
* The password for the encryption or decryption, in between single quotes `''`, or, the password file path that contains the password for the
  encryption or decryption.
* The source file path to encrypt or decrypt.
* The target file path to store the encrypted or decrypted file.

### With password

```bash
java -Dmode=<ENCRYPT|DECRYPT> -Dpassword=<password> -Dsource.file-path=<source-file-path> -Dtarget.file-path=<target-file-path> -jar file-cryptor-<VERSION>-runnable.jar
```

### With password file path

```bash
java -Dmode=<ENCRYPT|DECRYPT> -Dpassword-file-path=<password-file-path> -Dsource.file-path=<source-file-path> -Dtarget.file-path=<target-file-path> -jar file-cryptor-<VERSION>-runnable.jar
```

## Development

```bash
mvn clean install
```
