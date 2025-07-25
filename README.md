# Metafacture Playground

This is an approach to provide a web application to play around with Metafactures languages Fix, Flux and Morph inspired by the [JSON-LD Playground](https://json-ld.org/playground/).
This project is inititially created using the [leiningen re-frame template](https://github.com/day8/re-frame-template).

The ***production deployment*** is available at [https://metafacture.org/playground/](https://metafacture.org/playground/).

The current ***test deployment*** is available at [https://test.metafacture.org/playground/](https://test.metafacture.org/playground/).

Both deployments provide a web application and an HTTP API

[Here](CONTRIBUTING.md) you can read about contributing to Metafacture Playground.

## Docker installation

[Install Docker Engine](https://docs.docker.com/engine/install/) by follwing the guide for your distro.

[Install Docker Compose](https://docs.docker.com/compose/install/linux/#install-using-the-repository) as a plugin.

Alternatively, you may want to use the Docker Desktop environment which includes a GUI and all Docker components.

### Clone repo

```bash
git clone https://github.com/metafacture/metafacture-playground.git
cd metafacture-playground
```

### Start in development mode

```bash
docker compose -f docker-compose-dev.yml up
```

In development mode the ***local deployment*** is available at http://localhost:8280.

### Start in production mode

```bash
docker compose up
```

In production mode the ***local deployment*** is available at http://localhost:3000. 

Use the `--detach` option to run containers in the background, recommended when deploy on servers.

#### Resources limits

Limits for max number of CPUs and RAM usage are set in `docker-compose.yml`.
Currently limits are set to 3 CPUs and 8GB RAM. 

Java 11 processes are per default allowed to use up to 25% of the availbale RAM if Xmx is not set.
This means that the playground process can use up to 2GB java heap space.

### View output (logs) from Docker container

```bash
docker compose logs --timestamps
```

For more options see https://docs.docker.com/reference/cli/docker/compose/logs/

## Manual installation

Before starting you need to install [Leiningen](https://leiningen.org/) and a JDK (minimum Java 11).

### Install Leiningen

General setup on Unix (see [https://leiningen.org/](https://leiningen.org/) for other options):

```bash
mkdir ~/bin
wget -O ~/bin/lein https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
chmod a+x ~/bin/lein
```

### Install Metafacture Core

This project depends on [metafacture-core](https://github.com/metafacture/metafacture-core), which is work in progress.
It is possible to display the current version of dependencies in the UI. To display the version, please read this [section](#show-dependency-versions-in-ui). This is especially reasonable when installing on a server.

Clone and install metafacture-core:
```bash
$ git clone https://github.com/metafacture/metafacture-core.git
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

### Clone the metafacture-playground project

```bash
$ git clone https://github.com/metafacture/metafacture-playground.git
$ cd metafacture-playground
```

### Move Java policy file

The Metafacture Playground uses the [Java Security Manager](https://docs.oracle.com/javase/tutorial/essential/environment/security.html), so you need to configure proper permissions to run the Metafacture Playground.
Please move `.java.policy_move_to_home_dir` from the project's resources to your user's home directory and remove the suffix '_move_to_home_dir'.
Please adapt in the Java policy the lines concerning the file '.project' like described in the policy.
If there are problems starting and/or running the Playground, to find the problem it may help to add in the project.clj under the key ':jvm-opts' the entry '"-Djava.security.debug=access"' to see if a permission is missing.

### Start in development mode

When using development mode you don't have to restart when changing files. They will be reloaded automatically.

```
lein watch
```

Wait a bit, perhaps 20 seconds, keeping an eye out for a sign the compile has finished, then browse to http://localhost:8280.

### Start in production mode
Set the java version to 11, like:

```bash
export JAVA_CMD=/usr/lib/jvm/java-11-openjdk-amd64/bin/java
```

To test, compile and build the frontend run:

```
lein test; lein release
```

Then start the server with:

```
lein run
```

Browse to http://localhost:3000.

### Show dependency versions in UI

When installing the Metafacture Playground to a server it's important for users to know which version of Metafacture Core is used to process the workflows in the playground.

![Display version of dependency](/resources/img/displayVersions.JPG)

To display the version (or any other dependency of the playground) you have to put the file with the corresponding dependency name into the folder ```resources/versions```, e.g. the dependency of Metafacture Core is named ```org.metafacture/metafacture-framework``` in the project.clj, so we need a file named ```metafacture-framework``` in the folder ```resources/versions``` to display the version used in the project.clj in the UI.
The content of this file is a URI that should link to the corresponding version or branch commit and should be adapted manually. In the future the content of these files should be adapted automatically when installing Metafacture Core on the server where the playground is running.

#### Use a release version
If a released version is used, the content of the file contains the link to the release, e.g. [https://github.com/metafacture/metafacture-core/releases/tag/metafacture-core-7.0.0](https://github.com/metafacture/metafacture-core/releases/tag/metafacture-core-7.0.0).

#### Use Master/Main or other branch
If the master/main or another branch is used, the content of the file should contain a link to the commit like [https://github.com/metafacture/metafacture-fix/commit/b36fcb9](https://github.com/metafacture/metafacture-fix/commit/b36fcb9) (Please use the short hash link).

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

You also can run workflows on the web server without the web application, passing the GET parameters

`data` with value
```
1{a: Faust, b {n: Goethe, v: JW}, c: Weimar}
2{a: Räuber, b {n: Schiller, v: F}, c: Weimar}
```
,`flux` with value
```
inputFile
|open-file
|as-lines
|decode-formeta
|fix(transformationFile)
|encode-xml(rootTag="collection")
|print
;
```
and `transformation` with value
```
move_field(_id, id)
move_field(a, title)
paste(author, b.v, b.n, '~aus', c)
retain(id, title, author)
```
The parameter values must be URL-encoded so the URL looks like this:

http://localhost:3000/process?flux=inputFile%0A%7Copen-file%0A%7Cas-lines%0A%7Cdecode-formeta%0A%7Cfix%28transformationFile%29%0A%7Cencode-xml%28rootTag%3D%22collection%22%29%0A%7Cprint%0A%3B&transformation=move_field%28_id%2C+id%29%0Amove_field%28a%2C+title%29%0Apaste%28author%2C+b.v%2C+b.n%2C+%27~aus%27%2C+c%29%0Aretain%28id%2C+title%2C+author%29&data=1%7Ba%3A+Faust%2C+b+%7Bn%3A+Goethe%2C+v%3A+JW%7D%2C+c%3A+Weimar%7D%0A2%7Ba%3A+R%C3%A4uber%2C+b+%7Bn%3A+Schiller%2C+v%3A+F%7D%2C+c%3A+Weimar%7D

## Use Metafacture Playground API

Beside the web application the Metafacture Playground provides a HTTP API to process workflows. All you need is to provide the workflow information as JSON parameters in your POST request.

```
curl -X POST https://metafacture.org/playground/process -H 'Content-Type: application/json' -d '{"data":"1{a: Faust, b {n: Goethe, v: JW}, c: Weimar}\n2{a: Räuber, b {n: Schiller, v: F}, c: Weimar}","flux":"inputFile\n|open-file\n|as-lines\n|decode-formeta\n|fix(transformationFile)\n|encode-xml(rootTag=\"collection\")\n|print\n;","transformation":"move_field(_id, id)\nmove_field(a, title)\npaste(author, b.v, b.n, \"~aus\", c)\nretain(id, title,
 author)\n"}'
```
