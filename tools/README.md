# Tools Repository

This repository contains tools that are not directly part of the e-voting solution, but perform essential steps before or after the actual voting
event.

They contain the following tools:

| Tool               | Description                                                                                                             |
|--------------------|-------------------------------------------------------------------------------------------------------------------------|
| direct-trust-tool  | Tool with a command-line and a graphical interface generating the certificates and keystore to ensure channel security. |
| dispute-resolver   | Command-line tool for resolving a dispute of the list of confirmed votes between the Control Components.                |
| file-cryptor-tool  | Command-line tool for symmetrically encrypting and decrypting files.                                                    |
| xml-signature tool | Stand-alone tool generating and verifying digital signatures using the direct trust approach.                           |

## Usage

Tools are stand-alone components that one executes using a command-line or graphical interface.

## Development

Check the build instructions in the readme of the repository 'e-voting' for compiling the components.
