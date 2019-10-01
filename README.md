# Dotty Issue Runner
This is a tool to debug Dotty issues. It is an SBT plugin that allows you to write SBT commands in a text file like this:

```scala
dotty-bootstrapped/dotc
  $here/Macros_1.scala

dotty-bootstrapped/dotc
  -Xprint:staging,reifyQuotes
  -Xprint-inline
  -Ycheck:all
  -Yprint-pos
  -classpath .
  $here/Macros_2.scala
```

And have them executed in an SBT console in the rewritten, proper SBT form:

```scala
;dotty-bootstrapped/dotc  /Users/anatolii/Projects/dotty/pg/i7322/Macros_1.scala;dotty-bootstrapped/dotc  -Xprint:staging,reifyQuotes  -Xprint-inline  -Ycheck:all  -Yprint-pos  -classpath .  /Users/anatolii/Projects/dotty/pg/i7322/Macros_2.scala
```

## Usage
It assumes you work on the issues under the following directory structure:

```
workspace
├── i1
│   ├── Macros_1.scala
│   ├── Macros_2.scala
│   └── launch.iss
├── i2
│   ├── Macros_1.scala
│   ├── Macros_2.scala
│   └── launch.iss
└── i7322
    ├── Macros_1.scala
    ├── Macros_2.scala
    └── launch.iss
```

So you have a dedicated workspace folder (`workspace`) and each issue has its own sub-folder inside that workspace (`i1`, `i2`, `i7322`). The issue folders contain the issue sources and the launch scripts (`launch.iss`).

First, run `install.sh` to copy the `IssueRunner.scala` into your global SBT plugins directory.

Next, run `sbt` console from the Dotty folder. Two new commands are available:

- `issuesWorkspace /path/to/issue/workspace` – lets Dotty know where your issues are located
- `issue i1` – reads the SBT commands from `/path/to/issue/workspace/i1/launch.iss`, translates them to a one-liner SBT command and executes it from SBT.

## Launch Script Syntax
The launch script syntax is the same as the SBT commands one with the following considerations:

- Newlines are removed from the file contents
- If a line starts with a non-whitespace character, ";" will be added at the beginning of that line. Otherwise the line is unchanged. This allows to write command arguments on separate lines if they are indented.
- `#` starts a comment.
- `$here` string is replaced by the absolute path to the directory of the issue.
