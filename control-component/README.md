# Control Components
The control components generate the return codes, shuffle the encrypted votes, and decrypt them at the end of the election event. The Swiss Post Voting System uses four control components. 

## Usage
The control components modules are packaged in a single standalone SpringBoot applications.

## Development

```
mvn clean install
```

## Run

Certain operations run significantly faster using native optimizations. You can check the [crypto-primitives readme](https://gitlab.com/swisspost-evoting/crypto-primitives/crypto-primitives) for configuring native library support. 
