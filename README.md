# Energy Agents

[![DOI](https://zenodo.org/badge/81457869.svg)](https://zenodo.org/badge/latestdoi/81457869)

A micro-simulation model for determining residential energy demands in urban energy systems.

Each dwelling and each occupant in urban built environments is simulated individually.

## Use it

Download a jar file with all dependencies from the GitHub release page or build it on your own, see below.

`energy-agents` uses SQLite databases as scenario definition / input and as output. As soon as you have an input file you can run it through the command line interface:

    java -jar energy-agents.jar -i scenario.db -o results.db

You can also define the number of parallel threads to be used with the `-w` command line option.

Input files contain parameters for each dwelling and occupant, simulation parameters, and a time series of temperature values. As a reference have a look at the demo scenario `./energy-agents/src/test/resources/test-scenario.db`.

## Build it

`energy-agents` uses Maven, so the easiest way to build or test it is through Maven. You can build a jar file with all dependencies by running `mvn package`.
