# Dotty Issue Workspace
![CI](https://github.com/anatoliykmetyuk/dotty-issue-workspace/workflows/CI/badge.svg)

<p align="center">
  <img src="demo.gif">
</p>

*Please note that the above gif was recorded a long while ago, and the compiler commands like `dotty-bootstrapped/dotc` have since changed, e.g. to `scala3-bootstrapped/scalac`. The gif is for concept demonstration only.*

Dotty Issue Workspace is a small SBT-based build tool for issue reproduction in Dotty. It is implemented as an SBT plugin. It allows you to write a script that describes what needs to be done to reproduce an issue. This script currently supports SBT commands, shell commands and variables.

- [How it works](#how-it-works)
- [Usage](#usage)
- [Getting started](#getting-started)
- [Launch Script Syntax](#launch-script-syntax)
- [Advanced features](#advanced-features)
  * [Script variables](#script-variables)
  * [Shared launch scripts](#shared-launch-scripts)
  * [Nested issues](#nested-issues)

<small><i><a href='http://ecotrust-canada.github.io/markdown-toc/'>Table of contents generated with markdown-toc</a></i></small>

## How it works
Say you have an issue that reproduces by compiling two files, `lib.scala` and `Test.scala`, with Dotty. The second file needs the first file on the classpath. You can write the following script that will be understood by the Dotty Issue Runner:

```bash
# Compile the two files using SBT commands
scala3-bootstrapped/scalac -d $here $here/lib.scala
scala3-bootstrapped/scalac -d $here
  -classpath $here
  -Xprint:typer # Print typer for the second file
  $here/Test.scala

# We can also use shell commands
$ echo "Running the project now"

scala3-bootstrapped/dotr -classpath $here Test
```

The script is executed from the SBT console opened in the Dotty repository, so all the SBT commands are resolved against the Dotty SBT project.

The script above contains two SBT commands. Indented lines are joined with the lines without indentation using spaces, e.g. the first command becomes `scala3-bootstrapped/scalac -d $here $here/A.scala`. `$here` is a magic variable that points to the directory where the script resides.

For more examples, see [tests](https://github.com/anatoliykmetyuk/dotty-issue-workspace/tree/master/src/test/scala/dotty/workspace/core).

## Usage
Every issue has a dedicated folder with all the Scala files and the launch script residing there:

```
dummy
├── lib.scala
├── Test.scala
└── launch.iss
```

All of the issue folders reside in one parent folder, the Issue Workspace folder. The plugin obtains the location of the workspace folder from a config file.

From SBT console opened in the Dotty repo, you can then run `issue dummy` and the plugin will execute the `launch.iss` file found in that folder.

## Getting started
1. `echo -n 'addSbtPlugin("com.akmetiuk" % "dotty-workspace" % "0.1.0")' > ~/.sbt/1.0/plugins/dotty-issues-workspace.sbt` – add the plugin globally to SBT. If the directory structure does not exist, create it.
2. `echo -n 'workspace_path = /path/to/your/issue/workspace' > ~/.sbt/1.0/plugins/dotty-issues-workspace` – create the config file with the path to the workspace directory.
3. Navigate to the Dotty repo and run `sbt` command to open the SBT console.
4. From the SBT console, run `issue issue_folder_name`. This command will read the commands from `/path/to/your/issue/workspace/issue_folder_name/launch.iss` and executes them.

## Launch Script Syntax
The launch script syntax is as follows:

- Every command is on a new line, without indentation.
- If a line is indented, it gets appended to the previous line.
- `$ <cmd>` – executes a command using `bash`
- `cd <dir>` – sets the working directory where `$` executes commands
- `val <name> = <value>` – defines a variable. You can use variables in commands via `$name`.
- `# Comment` – a comment
- Everything else is interpreted as an SBT command.

## Advanced features
### Script variables
Normally you launch an issue using `issue issue_name` command. You can supply extra arguments to this command: `issue issue_name arg_1 arg_2`. These arguments are available inside the script as variables via `$<arg_id>`, e.g. `$1`, `$2` etc.

For example, consider the following script:

```bash
scalac $here/iss_$1.scala
```

If you launch it as `issue issue_name compiler_crash`, the script will translate into a single SBT command: `scalac $here/iss_compiler_crash.scala`.

### Shared launch scripts
Normally, each issue contains its own launch script `launch.iss`. If, however, such a script is missing, the plugin will attempt to load such a script from parent directories. E.g. you can have the following setup:

```
workspace
├── issue-1
│   ├── Test.scala
│   ├── launch.iss
│   └── lib.scala
├── issue-2
│   └── Test.scala
└── launch.iss
```

If you call `issue issue-1`, the `workspace/issue-1/launch.iss` script will be executed. If you call `issue issue-2`, the `workspace/launch.iss` script will be executed since `issue-2` doesn't have its own script.

The `$here` variable always points to the issue folder, even if the launch script resides in one of the parent directories. This means that `$here` for `issue-1` will be `workspace/issue-2` and for `issue-2` – `workspace/issue-2`.

### Nested issues
The ability to share launch scripts leads to the possibility to have a directory structure where one issue has multiple subissues, as follows:

```
workspace
└── nested
    ├── iss1
    │   ├── Macro.scala
    │   └── Test.scala
    ├── iss2
    │   ├── Macro.scala
    │   └── Test.scala
    └── launch.iss
```

You can call the above issues as `issue nested/iss1` and `issue nested/iss2` respectively.

This can be useful when you work with several close reproductions of the same root issue.
