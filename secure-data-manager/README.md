# Secure Data Manager

The Secure Data Manager implements the setup component's and tally control component's algorithms of the Swiss Post Voting System. In the
configuration phase, the setup component combines the control components' contributions and generates the codes. In the tally phase, the tally control
component performs the final mixing and decryption of the votes.

The protocol requires **different** instances of the Secure Data Manager:

- The Secure Data Manager for the configuration phase (Setup component functionality)
- The Secure Data Manager for the tally phase (Tally control component functionality)
- The online Secure Data Manager for transferring data between the Secure Data Manager and the voting server/control components

The Secure Data Manager's execution must fulfill the following conditions.

- The Secure Data Manager is operated by the cantons, **not** by Swiss Post.
- The setup component and tally control component Secure Data Manager instances are **offline**. They transfer data only via secure USB to the online
  Secure Data Manager.
- The machines running the Secure Data Manager are hardened and have no other purpose than running the Secure Data Manager software.
- The cantonal administrator stores the Secure Data Manager machines securely during the voting phase.

We assume that the setup component Secure Data Manager and the tally control component Secure Data Manager do *not* share confidential data and that
the information they write in their internal file system remains secret.

## Usage

The Secure Data Manager has a SpringBoot backend and an Angular frontend, deployed on [Electron](https://www.electronjs.org/). The frontend
interacts with the Secure Data Manager backends via HTTP calls.

In general, the Secure Data Manager heeds web application security best practices when appropriate. However, we do not enforce authentication between
the application's frontend and backend parts, and we omit HTTP security headers. Please note that while the Secure Data Manager uses web technologies
for the user interface, the Secure Data Manager Backend accepts only local traffic. If the adversary controls the Secure Data Manager instance, he
could access the internal file system, and sniffing the local HTTP traffic would be pointless. To prevent an attacker from controlling a Secure Data
Manager instance, we implement the operational safeguards described above.

## Development

Check the build instructions in the readme of the repository 'e-voting'.

## Run

Certain operations run significantly faster using native optimizations. You can check
the [crypto-primitives readme](https://gitlab.com/swisspost-evoting/crypto-primitives/crypto-primitives) for configuring native library support.