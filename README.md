Yes I know it's jank AF. Too bad!

# Choose Your Own Adventure Engine
## This program is designed to make creating CYOA type stories easy.
![image](https://github.com/nickolasbradham/CYOA-Engine/assets/105989209/8e0ba0fa-330e-4f1e-9a4a-03e908b8cb20)

The engine can load `.coa` text files which are just text files with the extension changed.

You can see `Example Story.coa` for a brief example of the majority of what this engine provides in action.
## General COA File Flow
The file is parsed from top to bottom a line at a time. If a line starts with a command token (see farther down for commands), the command is executed.
If no command token is present (or it's escaped with `\` at the start), the line is just sent to the text area for the player.

Text displayed to the reader can contain a variable (more on them in the next section) name surrounded by `<` and `>` to insert a variable value in the text.

In the following example `>h` is the command token for a header and the rest of the line is processed differently (see farther down for that command), but the next two lines will just be sent to the text area.
```
>h Chapter 1: <name> Awakens...
You wake up in what appears to be an old rusty cell. Sun light filters through the tiny windows from outside the cell.
It is quiet and you don't see anyone around.
```
## Variables
Variables can also affect the flow of the story. There are three main vairable types: Flags, Numbers, and Strings

`Flags` are simple in that they exist or don't exist. They don't store data.

`Numbers` can contain a number that can be operated on or compared.

`Strings` sadly have very limitted use. You can take input from the reader, display strings, and test equality on them.
## Expressions
Some commands can take expressions. These take the form of `[!]<flag>` or `<varA><comparison><val | varB>`. Do note there is no space between variables and ops.

`<flag>` will return true if "flag" exists.

`!<flag>` will return true if "flag" does NOT exist.

**Comparisons:**

`=` Equal

`!` NOT equal

`<` Less than

`[` Less than OR equal to

`>` Greater than

`]` Greater than OR equal to
## Commands
### `>c` Clear
Clears the text area.
### `>cf` Clear Flags/Variables
Deletes all story flags and variables.
### `>f <name> [value]` Create Flag/Variable
`[value]` is parsed to the end of the line (i.e. it can have spaces).

Creates a flag wth name `<name>` if `[value]` is not supplied.

Creates a Number if `[value]` contains a number.

Creates a String containing `[value]` if previous case is not met.

**Examples:**

`>f tookKeys` will create a flag labeled "tookKeys".

`>f bullets 3` will create a number variable named "bullets" and set it to 3.

`>f secret Best secret password` will create a String variable named "secret" and set it to "Best secret password".
### `>f <nameA><op><nameB | val>` Perform variable operation.
Performs `<op>` on `<nameA>` and `<nameB | val>`. Then stores result in `<nameA>`. Do note that there is no space between the names and operator.

**Operators:**

`+` Addition

`-` Substraction

`*` Multiplication

`/` Division

`%` Remainder

`:` Copy (Copies the value of `<nameB | val>` into `<nameA>`)

**Examples:**

`>f bullets-1` will subtract 1 from variable "bullets".

`>f cash+money` will add variable "money" to variable "cash".

`>f before:message` will copy variable "message" into variable "before".
### `>fd <name>` Delete Flag/Variable
Deletes flag/variable `<name>`.

**Examples:**

`>fd tookKeys` would delete the "tookKeys" flag.
### `>h <text>` Set Header
Sets the header text to `<text>`.

`<text>` is parsed to the end of the line (i.e. it can have spaces) and it can have variables inserted in it.

**Examples:**

`>h Chapter 1: <name> Awakens!` would set the header text to "Chapter 1: Nick Awakens!" assuming there was variable "name" that had the text "Nick" stored in it.
### `>i <name> <prompt>` Get input from the reader.
Displays a input text box to the reader with `<prompt>` displayed. The input will be stored in variable `<name>`.

`<prompt>` is parsed to the end of the line (i.e. it can have spaces) and it can have variables inserted in it.

**Examples:**

`>i reader What is your name?` Would prompt "What is your name?" and store the input in the variable "reader"
### `>j <label>` Jump to label
Jumps story to next label `<label>` or loops to start of story and checks again.

This is used in conjunction with `>l` (see that command) to jump to different story places.

**Examples:**

`>j ending1` Jumps the story to label "ending1"
### `>jf <expression> <label>` Jump if expression is true
If `<expression>` is true, jumps story to next label `<label>` or loops to start of story and checks again.

This is used in conjunction with `>l` (see that command) to jump to different story places.

**Examples:**

`>jf key unlockDoor` If flag "key" exists, jumps the story to label "unlockDoor".

`>jf !cutPower startMachine` If flag "cutPower" does NOT exist, jumps the story to label "startMachine".

`>jf timeWaited>3 tooLate` If variable "timeWaited" is greater than 3, jumps the story to label "tooLate".

### `>l <label>` Story label
Defines a label to jump to.

Used in conjunction with `>j`, `>jf`, `>o`, and `>of` (see those commands) to jump to different story places.

**Examples:**

`>l ending1` Defines label "ending1" for jumping to.

//TODO Finish docs.
