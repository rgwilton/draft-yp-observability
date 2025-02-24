# YANG Push Operational Data Observability Enhancements

This is the working area for the individual Internet-Draft, "YANG Push Operational Data Observability Enhancements".

* [Editor's Copy](https://rgwilton.github.io/draft-yp-observability/#go.draft-wilton-netconf-yp-observability.html)
* [Datatracker Page](https://datatracker.ietf.org/doc/draft-wilton-netconf-yp-observability)
* [Individual Draft](https://datatracker.ietf.org/doc/html/draft-wilton-netconf-yp-observability)
* [Compare Editor's Copy to Individual Draft](https://rgwilton.github.io/draft-yp-observability/#go.draft-wilton-netconf-yp-observability.diff)

This respository uses two separate build systems (and associated build files):
 - *mill* is used to fetch, compile, and check the YANG files.
 - *make* is used to convert the markdown to HTML and txt using Martin Thomson's id-template repository.

Currently, only the make step is build into github actions, the mill step needs to be run on a laptop or pc.

## Contributing

See the
[guidelines for contributions](https://github.com/rgwilton/draft-yp-observability/blob/main/CONTRIBUTING.md).

Contributions can be made by creating pull requests.
The GitHub interface supports creating pull requests using the Edit (✏) button.


## Compiling building YANG and tree snippets using *mill*

All the YANG files are built using a *mill* build script, which will fetch all dependencies, and compile the YANG and extract the tree diagrams.  The generated tree diagrams are written to generated-tree-output, which are committed so that the make based tooling works.

**You must not edit any files in the *generated-tree-output* directory because they will be overwritten the next time a build using *mill* is performed, instead edit the source YANG file in the *yang* directory or the *build.mill* build file to change what YANG tree output is built and what snippets or tree output are generated.**

To build everything, then from the project root run:

```sh
$ ./mill all
```

Or, to continuously watch for changes to any source files and automatically rebuild:

```sh
$ ./mill --watch all
```

To check for if any draft dependencies are out of date:

```sh
$ ./mill checkDraftLatest
```

To see full set of build targets:

```sh
$ ./mill resolve _
```

To clean:

```sh
$ ./mill clean
```

## Command Line Usage

Formatted text and HTML versions of the draft can be built using `make`.

```sh
$ make
```

Command line usage requires that you have the necessary software installed.  See
[the instructions](https://github.com/martinthomson/i-d-template/blob/main/doc/SETUP.md).

## Further details

If you need to debug any issues with the mill build, then all the intermediate mill build files get written into the ```out``` directory under separate \<task\>.Dest target folders.

Currently the workspace uses two *mill* build files:

- ***build.mill*** - contains all the main settings for the project.
- *util_ietf.mill* - contains the underlying build rules, this should be generic and eventually move out of the project and be downloaded on demand.