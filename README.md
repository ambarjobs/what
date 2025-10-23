# what

This is a small CLI utility to store and retrieve information about other CLI utilities and applications.

## Motivation

Usually, specially on Linux platforms, you have a bunch of small CLI utilities, applications and scripts that have once been installed to solve a problem or to be a valuable tool in our daily job.

With the time we forget about some of them or forget how to use them or where to find information about them, specially for those we don't use regularly.

`what` is an attempt on provide such information easily on the terminal itself.

## Further motivation

An additional reason to create this utility is that this is my first "real" (in the sense of something that has a purpose and isn't just for study) program in Clojure.

Some time ago I have implemented it in bash, with the corresponding limitations and inflexibility, then I resolved to implement it in Clojure to help in fixing the language concepts that I have been studying recently, extending it a bit.

## Installation

The utility can be installed on any directory by cloning this project:

```shell
git clone https://github.com/ambarjobs/what.git
```

It's useful to create a symlink to the executable (`what`) in a directory in your path:

```shell
cd what
sudo ln -s $(pwd)/what /usr/bin/what
```

Also you'll need to install [Babashka](https://babashka.org/) because as explained in [Description](#description), this is interpreted code (but with a fast startup time then that provided by a JVM).

Here you can find information on how [install Babashka](https://github.com/babashka/babashka#installation).

### Requirements

In addition to the libraries already provided by Babashka the following libraries (as configured in `bb.edn`) will be installed:
- `org.babashka/go-sqlite3`: Babashka Pod for interacting with SQLite3
- `com.github.seancorfield/next.jdbc`: Clojure wrapper for JDBC-based access to databases
- `com.github.seancorfield/honeysql`: SQL as Clojure structures

## Usage

If you run `what` without parameters you get a usage message showing all the available `actions` to be applied to the `command`s (other CLI utilities).

With exception of `list`, the other actions receive the command name as an additional parameter.
I think that this makes the call to the utility feel more natural than using options (e.g.: `what is dig`).

Below there is a description of available actions:

- **list**: List all commands with their descriptions.
- **is**: Show the description of a specific command.
- **doc**: Open the documentation of the command, be it an option (e.g. `delta --help`)  or a man page (e.g. `man jq`) with pagination when the information don't fits the terminal.
- **find**: Queries for a string on all or specific fields, returning information about the found commands.
- **add**: Adds a new command into the database.
- **upd**: Updates information of an existing command.
- **rm**: Removes a command from the database.
- **urls**: Show the URLs associated with a command. Usually these URLs point to command's Github page or documentation site.
- **help** | **h** | **?**: Show the usage information.

When adding or updating a command record it's possible to register a name for the program if it is different from the command name.
This name will be shown on the description of the command between brackets:

```
trip: [Trippy] Network connection analyzer combining traceback and ping.
```

At the moment there is no actions to add or remove URLs for the commands (this have to be done directly on the database), but opportunely they'll be added.

## Description

Clojure is a Lisp dialect implemented using Java's JVM.

For this reason the startup time is too long for a small utility.

So I found [Babashka](https://babashka.org/) that provides a JVM free Clojure implementation by interpreting directly the source code.

The startup time of the initial implementation in Clojure was reduced from about 4 seconds to lees than 170 ms using Babashka.

For this to work it's necessary to install Babashka on your environment (see [Installation](#installation)).

To persist information about the other CLI utilities I've used a small SQLite database on the project classpath (accessed as a resource).

Babashka doesn't support Java libraries and I couldn't find a pure Clojure library to access SQLite database.

The solution was to use a [a Go SQLite connector through a Babashka Pod](https://github.com/babashka/pod-babashka-go-sqlite3).
Babashka Pods are a kind of interfaces between Babashka (Clojure) and applications that may have been written in different languages.

There is a simple `config.edn` configuration file in the `resources` directory that isn't intended for the utility user (just for developer configuration) with the pager to be used for long docs and with the path to the database file.

The command field is required and must be unique.
Description is required too, the rest of fields are optional.

In addition to the actions shown on the usage notice, there is a hidden `dbg` action that calls a handler for debugging and experimentation purposes.

## Unit Tests

The unit tests are in the `test` directory.

They can be run through `run_tests`, that's an executable on project's root directory.

## Limitations

As I've told you before this is my first attempt on writing a Clojure (Babashka) program.

So you can expect to find less idiomatic code or something that could be implemented better by someone with more mileage on the language.

My (fragile) consolation is that the tests are somewhat comprehensive and are all passing 🙂.

User interface handling is a hard task. I think I've covered much of the cases but always there is room for mistakes.

## License

Copyright © 2025, Armando M. Baratti

Distributed under MIT Style NON-AI License. See [LICENSE](LICENSE.md)
