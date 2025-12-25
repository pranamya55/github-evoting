# Voting Client

The voting-client builds upon the crypto-primitives-ts library and defines a frontend API for the voter-portal. The crypto-primitives-ts
library implements the underlying cryptographic primitives.

The voting-client provides an API, which can be invoked by the front-end voter-portal.

## Usage

The build process generates an obfuscated and minimized bundle file called `ov-api.min.js`, which can be used directly, or published to a repository.
After publication, this package can be accessed as an NPM dependency via the package name `ov-api`.
&nbsp;

## Build instructions

See also the build instructions in the readme of the repository 'evoting' for compiling the voting-client module.

### Prerequisites

Make sure that the following third party software is installed:

#### Windows

- `Node.js`
- `Mozilla Firefox`
- `Google Chrome`

#### Linux (Operating system)

- `Node.js`
- `Mozilla Firefox`
- `Google Chrome`

#### Linux (Windows Subsystem for Linux (WSL))

- `Node.js`

**NOTE:** When using WSL, you will have to set the environment variable `CHROME_BIN` to the path of your Windows Google Chrome executable file.
For a standard Chrome installation, this can be achieved by adding the following line to your shell startup file `$HOME/.profile`:

```text
export CHROME_BIN="/mnt/c/Program Files (x86)/Google/Chrome/Application/chrome.exe"
```

### How to install dependencies

Change to the top level directory of this package and install its dependencies as:

```text
npm install
```

### How to build package and run tests

From the top level directory of this package, build it and run the tests as:

```text
npm run build
npm run test
```