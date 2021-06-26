### Make commands:
`make compile` Compiles the Lox interpreter.

`make run` Runs the interpreter interactively.

`make run-script path=path/to/script/file` Runs the script present in the path.

### Data Types:
NUMBER - can be integer or floating
STRING - sequence of characters
BOOLEAN - true/false
NULL - nil


### Language Syntax:

#### 1. Print a statement
`print 1 + 2;`

---
#### 2. Variable declaration:
- `var x = 2;`
- `var s = "happy"`

#### 3. Branching Control Flow
###### IF condition:
<pre>
if (x == 2) {
    print "x equals 2";
}
else if (x == 3) {
    print "x equals 3"
}
else {
    print "x is neither 2 nor 3";
}
</pre>

#### 4. Loop
###### WHILE loop
<pre>
var x = 2;
while (x < 10) {
    print x;
    x = x + 1;
}
</pre>

###### FOR loop
<pre>
for (var i = 0; i < 10; i = i + 1) print i;
</pre>

#### 5. Functions
TODO