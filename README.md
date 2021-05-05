# Metafacture Playground

This is an approach to provide a web application to play around with Metafactures languages Fix and Flux inspired by the [JSON-LD Playground](https://json-ld.org/playground/).
This project is inititially created using the [leiningen re-frame template](https://github.com/day8/re-frame-template).

The current test deployment is available at [http://test.lobid.org/playground/](http://test.lobid.org/playground/)

## Installation

Before starting you need to install [Leiningen](https://leiningen.org/) and a JDK (minimum Java 8).

### Install Leiningen

General setup on Unix (see [https://leiningen.org/](https://leiningen.org/) for other options):

```bash
mkdir ~/bin
wget -O ~/bin/lein https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
chmod a+x ~/bin/lein
```

### Install Metafacture Fix

This project depends on [metafacture-fix](https://github.com/metafacture/metafacture-fix), which is work in progress.

Clone and install metafacture-fix:
```bash
$ git clone https://github.com/metafacture/metafacture-fix.git
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

### Clone the metafacture-playground project

```bash
$ git clone https://github.com/metafacture/metafacture-playground.git
$ cd metafacture-playground
```

### Start in development mode

When using development mode you don't have to restart when changing files. They will be reloaded automatically.

```
lein watch
```

Wait a bit, perhaps 20 seconds, keeping an eye out for a sign the compile has finished, then browse to http://localhost:8280.

### Start in production mode

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

[http://localhost:3000/process?data='1'{'a': '5', 'z': 10}&flux=as-lines|decode-formeta|fix|encode-formeta(style="multiline")&fix=map(a,b) map(_else)](http://localhost:3000/process?data=%271%27{%27a%27:%20%275%27,%20%27z%27:%2010}&flux=as-lines|decode-formeta|fix|encode-formeta(style=%22multiline%22)&fix=map(a,c)%20map(_else))

### Run tests

### clj tests

Install karma and headless chrome

```
npm install -g karma-cli
```

Point to your Chrome binary, e.g.

```
export CHROME_BIN='/usr/bin/chromium-browser'
```

And then run your tests

```
lein watch
```

And in another terminal:

```
karma start
```

### clj tests

Run

```
lein test
```

## Run workflows on the web server

Run workflows on the web server, passing `data`, `flux`, and `fix` as GET-Parameter:

[http://localhost:3000/process?data='1'{'a': '5', 'z': 10}&flux=as-lines|decode-formeta|fix|encode-formeta(style="multiline")&fix=map(a,b) map(_else)](http://localhost:3000/process?data=%271%27{%27a%27:%20%275%27,%20%27z%27:%2010}&flux=as-lines|decode-formeta|fix|encode-formeta(style=%22multiline%22)&fix=map(a,c)%20map(_else))
