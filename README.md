# Dotty Issue Workspace
![CI](https://github.com/anatoliykmetyuk/dotty-issue-workspace/workflows/CI/badge.svg)

<p align="center">
  <img src="demo.gif">
</p>

Dotty Issue Workspace is a small SBT-based build tool for issue reproduction in Dotty. It is implemented as an SBT plugin. It allows you to write a script that describes what needs to be done to reproduce an issue. This script currently supports SBT commands, shell commands and variables.

Say you have an issue that reproduces by compiling two files, `lib.scala` and `Test.scala`, with Dotty. The second file needs the first file on the classpath. You can write the following script that will be understood by the Dotty Issue Runner:

```bash
# Compile the two files using SBT commands
dotty-bootstrapped/dotc -d $here $here/lib.scala
dotty-bootstrapped/dotc -d $here
  -classpath $here
  -Xprint:typer # Print typer for the second file
  $here/Test.scala

# We can also use shell commands
$ echo "Running the project now"

dotty-bootstrapped/dotr -classpath $here Test
```

You can save it in a file `launch.iss` and execute it from SBT console of the Dotty repo.

The script above contains two SBT commands. Indented lines are joined with the lines without indentation using spaces, e.g. the first command becomes `dotty-bootstrapped/dotc -d $here $here/A.scala`. `$here` is a magic variable that points to the directory where the script resides.

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
4. From the SBT console, run `issue issue_folder_name`. This command will read the commands from `/path/to/your/issue/workspace/launch.iss` and executes them.

## Launch Script Syntax
The launch script syntax is as follows:

- Every command is on a new line.
- If a line is indented, it gets appended to the previous line without indentation.
- `$ <cmd>` – executes a command using `bash`
- `cd <dir>` – sets the working directory where `$` executes commands
- `val <name> = <value>` – defines a variable. You can use variables in commands via `$name`.
- `# Comment` – a comment
- Everything else is interpreted as an SBT command.

For example, see [tests](https://github.com/anatoliykmetyuk/dotty-issue-workspace/tree/master/src/test/scala/dotty/workspace/core).

### Example
Say I want to:

1. Compile a 3rd party project (utest) with Dotty
2. Compile a Scala file `test.scala` with utest on the classpath.

I can make the following script:

```bash
# Publish Dotty to the local ivy repo
dotty-bootstrapped/publishLocal

# Define variables pointing where utest is
val utest_dir = /Users/kmetiuk/Projects/scala3/tools/ecosystem/repos/utest/
val utest_classpath =
  $utest_dir/out/utest/jvm/0.24.0-bin-SNAPSHOT/compile/dest/classes

# Compile utest using mill
cd $utest_dir
$ ./mill
  -D dottyVersion="0.24.0-bin-SNAPSHOT"
  utest.jvm[0.24.0-bin-SNAPSHOT].compile

# Compile the example file
dotty-bootstrapped/dotc -d $here
  -classpath $utest_classpath
  $here/test.scala
```