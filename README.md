# Metafacture Playground

This is an approach to provide a web application to play around with Metafactures languages Fix, Flux and Morph inspired by the [JSON-LD Playground](https://json-ld.org/playground/).
This project is inititially created using the [leiningen re-frame template](https://github.com/day8/re-frame-template).

The ***production deployment*** is available at [https://metafacture.org/playground/](https://metafacture.org/playground/).

The current ***test deployment*** is available at [https://test.metafacture.org/playground/](https://test.metafacture.org/playground/).

[Here](CONTRIBUTING.md) you can read about contributing to Metafacture Playground.

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
It is possible to display the current version of dependencies in the UI. To display the version, please read this [section](#show-dependency-versions-in-ui). This is especially reasonable when installing on a server.

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

### Show dependency versions in UI

When installing the Metafacture Playground to a server it's important for users to know which version of Metafacture Core and Metafacture Fix are used to process the workflows in the playground.

![Display versions of dependecies](/resources/img/displayVersions.JPG)

To display these versions (or any other dependency of the playground) you have to put a file with the corresponding dependency name into the folder ```resources/versions```, e.g. the dependency of Metafacture Fix is named ```org.metafacture/metafix``` in the project.clj, so we need a file named ```metafix``` in the folder ```resources/versions``` to display the version used in the project.clj in the UI.
The content of this file is a URI that should link to the corresponding version or branch commit and should be adapted manually. In the future the content of these files should be adapted automatically when installing Metafacture Fix or Metafacture Core on the server where the playground is running.
To display the Metafacture Core dependency we use ```org.metafacture/metafacture-framework```.

#### Use a release version
If a released version is used, the content of the file contains the link to the release, e.g. [https://github.com/metafacture/metafacture-core/releases/tag/metafacture-core-5.3.1](https://github.com/metafacture/metafacture-core/releases/tag/metafacture-core-5.3.1).

#### Use Master/Main or other branch
If the master/main or another branch is used, the content of the file should contain a link to the commit like [https://github.com/metafacture/metafacture-fix/commit/b36fcb915377ec6d12c85520eca770fd4aa600de](https://github.com/metafacture/metafacture-fix/commit/b36fcb915377ec6d12c85520eca770fd4aa600de).

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
