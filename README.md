# Metafacture Playground

This is an approach to provide a web application to play around with Metafactures languages Fix and Flux inspired by the [JSON-LD Playground](https://json-ld.org/playground/).
This project is inititially created using the [leiningen re-frame template](https://github.com/day8/re-frame-template).

## Installation

Before starting yo need to install [Leiningen](https://leiningen.org/) and a JDK (minimum Java 8).
This project depends on [metafacture-core](https://github.com/metafacture/metafacture-core) and [metafacture-fix](https://github.com/metafacture/metafacture-fix). Clone and install both repositories (branch oersi).

Clone and install metafacture-fix:
```bash
$ git clone --branch oersi https://github.com/metafacture/metafacture-fix.git
$ cd metafacture-fix
```

Unix:
```bash
$ ./gradlew install
```

Windows:
```bash
$ .\gradlew.bat install
```
Clone and install metafacture-core:
```bash
$ git clone --branch oersi https://github.com/metafacture/metafacture-core.git
$ cd metafacture-core
```

Unix:
```bash
$ ./gradlew install
```

Windows:
```bash
$ .\gradlew.bat install
```

Clone the metafacture-playground project

```bash
$ git clone https://github.com/metafacture/metafacture-playground.git
$ cd metafacture-playground
```

### Start the application (only frontend):

```
lein watch
```

Wait a bit, perhaps 20 seconds, keeping an eye out for a sign the compile has finished, then browse to http://localhost:8280.

### Start the application (frontend + backend)
To compile and build the frontend run:

```
lein release
```
Then start the server with:

```
lein run
```
Browse to http://localhost:3000.

Run workflows on the web server, passing `data`, `flux`, and `fix`:

[http://localhost:8080/xtext-service/run?data='1'{'a': '5', 'z': 10}&flux=as-lines|decode-formeta|fix|encode-formeta(style="multiline")&fix=map(a,b) map(_else)](http://localhost:8080/xtext-service/run?data=%271%27{%27a%27:%20%275%27,%20%27z%27:%2010}&flux=as-lines|decode-formeta|fix|encode-formeta(style=%22multiline%22)&fix=map(a,c)%20map(_else))

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
runs under http://localhost:3000.
Run workflows on the web server, passing `data`, `flux`, and `fix`:

[http://localhost:8080/xtext-service/run?data='1'{'a': '5', 'z': 10}&flux=as-lines|decode-formeta|fix|encode-formeta(style="multiline")&fix=map(a,b) map(_else)](http://localhost:8080/xtext-service/run?data=%271%27{%27a%27:%20%275%27,%20%27z%27:%2010}&flux=as-lines|decode-formeta|fix|encode-formeta(style=%22multiline%22)&fix=map(a,c)%20map(_else))
