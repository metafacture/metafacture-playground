# Metafacture Playground

This is an approach to provide a web application to play around with Metafactures languages Fix and Flux inspired by the [JSON-LD Playground](https://json-ld.org/playground/).
This project is inititially created using the [leiningen re-frame template](https://github.com/day8/re-frame-template).

## Installation

Before starting yo need to install [Leiningen](https://leiningen.org/) and a JDK.

Clone the metafacture-playground project

```bash
$ git clone https://github.com/metafacture/metafacture-playground.git
$ cd metafacture-playground
```

### Start the application:

```
lein watch
```

Wait a bit, perhaps 20 seconds, keeping an eye out for a sign the compile has finished, then browse to http://localhost:8280.

### Run tests

Install karma and headless chrome

```
npm install -g karma-cli
```

And then run your tests

```
lein watch
```

And in another terminal:

```
karma start
```

### Production Build

To compile clojurescript to javascript:

```
lein release
```

Create uberjar

```
lein clean
lein uberjar
```

The jar startet with

```
java -jar metafacture-playground.jar
```
runs under http://localhost:3000
