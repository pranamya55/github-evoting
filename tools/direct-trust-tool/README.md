# Direct Trust Tool

The Direct Trust Tool helps users with common cryptographic task related to the e-voting process.

## 1. Overview

The following e-voting participants require a signing keystore.

- Canton
- Setup Component
- Tally Control Component
- Voting Server
- Control Component 1
- Control Component 2
- Control Component 3
- Control Component 4
- Printing Component
- Dispute Resolver

Each keystore contains the following :

- A private key to sign contents.
- All the trusted certificates of other components to validate the received content.

### 1.1. Alias

Each e-voting participant involved in signing is assigned an alias. This alias is used to identify the participant in the keystore.

| Participant             | Alias               |
|-------------------------|---------------------|
| Canton                  | CANTON              |
| Setup Component         | SDM_CONFIG          |
| Tally Control Component | SDM_TALLY           |
| Voting Server           | VOTING_SERVER       |
| Control Component 1     | CONTROL_COMPONENT_1 |
| Control Component 2     | CONTROL_COMPONENT_2 |
| Control Component 3     | CONTROL_COMPONENT_3 |
| Control Component 4     | CONTROL_COMPONENT_4 |
| Printing Component      | PRINTING_COMPONENT  |
| Dispute Resolver        | DISPUTE_RESOLVER    |

### 1.2. Process

The process to generate the keystores is as follows:

1. generate the keystore for the components you manage.
2. download the public keys of the generated keystores.
3. import the public keys of all the component into your keystores.
4. download your keystores.
5. clear the workspace.

## 2. GUI tool

The GUI tool provides a simple interface to generate the keystores. The wizard will guid you through all the steps of the process easily.

## 3. CLI tool

The commands it provides are:

| Command                      | Description                                          |
|------------------------------|------------------------------------------------------|
| keystores-generation         | Generate the wanted keystores.                       |
| public-keys-sharing-download | Download the public keys of the generated keystores. |
| public-keys-sharing-import   | Add the public keys to the keystores.                |
| public-keys-fingerprint      | Check the hashes of the public keys.                 |
| keystores-download           | Download the generated keystores.                    |
| clear                        | Remove a workspace and all its generated keystores.  |

### 3.1. Generating Direct Trust Keystores

First generate the keystore for the components you own by running a command like this:

```shell
java -jar direct-trust-tool-cli-<VERSION>-runnable.jar \
     keystores-generation \
     --components <string> \
     --valid-until <yyyy-MM-dd> \
     --country <string> \
     --state <string> \
     --locality <string> \
     --organization <string> 
```

This will initialize a keystore for each component you own.

Note that you can add the optional parameter `--platfrom` that will be appended to the name of the keystore, for instance to define its scope, like
TEST, PROD, etc.
By default, it will be blank.

## 3.2. Downloading Public Keys

After generating the keystores, you can download the public keys of the components you own by running a command like this:

```shell
java -jar direct-trust-tool-cli-<VERSION>-runnable.jar \
     public-keys-sharing-download \
     --output <path> 
```

This will create a zip containing all the public keys of the component you own.

## 3.3. Importing Public Keys

After downloading the public keys, you can import the consolidate key set into the keystores by running a command like this:

```shell
java -jar direct-trust-tool-cli-<VERSION>-runnable.jar \
     public-keys-sharing-import \
     --public-key-path <path> 
```

The path must be a directory containing all the public keys of all the components.

## 3.3. Check the hashes of Public Keys

Before importing the keystore, you can check the hashes of the public keys by running a command like this:

```shell
java -jar direct-trust-tool-cli-<VERSION>-runnable.jar \
     public-keys-fingerprint
```

## 3.4. Downloading Keystores

After importing the public keys, you can download the keystores by running a command like this:

```shell
java -jar direct-trust-tool-cli-<VERSION>-runnable.jar \
     keystores-download \
     --output <path> 
```

The output path is the directory where the keystores will be saved as zip.

## 3.5. Clearing Workspace

After finishing the process, you should clear the workspace by running this command:

```shell
java -jar direct-trust-tool-cli-<VERSION>-runnable.jar \
     clear 
```