# Dragonmark - Util

A bunch of Clojure utility functions and packages.

[![Clojars Project](http://clojars.org/dragonmark/util/latest-version.svg)](http://clojars.org/dragonmark/util)

The current build status:
<a href="https://travis-ci.org/dragonmark/util">
![Build Status](https://travis-ci.org/dragonmark/util.svg?branch=develop)</a>



## Helpful Utilities

### Vector helper functions

Same as `clojure.core` except for Vectors: `mapcatv` `removev`
`restv`
`concatv` and `consv`.

### `maptree`

Applies the function f to each node in the tree, bottom-up.
Find the children using child-key and if child-key is missing, use `:children`.

### `some-or`

Like `or` except returns the first computed value that is not nil... allows threading
`false` through.

### `exec-after`

execute a function after a certain number of milliseconds

### `next-guid`

Generate a monotonically increasing GUID with a ton of randomness.

## Properties

`dragonmark.util.props` is a nice properties file system that will look for
the properties file based on username, machine name, and run mode:

If the `property_file` environment variable is set, it
should be the name of the property file.

If the `/.dockerenv` file exists, the `/data/default.props` file
is checked. If you're running in a Docker container, you can
put the properties file in `/data/default.props` and it will be
used.

Files are searched in this order:

```
    (str "/props/" mode-name "." user-name "." host-name)
    (str "/props/" mode-name "." user-name)
    (str "/props/" mode-name "." host-name)
    (str "/props/" mode-name ".default" )
    (str "/props/" user-name )
    "/props/default" 
    (str "/" mode-name "." user-name "." host-name)
    (str "/" mode-name "." user-name)
    (str "/" mode-name "." host-name)
    (str "/" mode-name ".default" )
    (str "/" user-name )
    "/default"
```

So `/props/dpp.props` is my properties file. But `test.default.props`
is the properties file that contains properties used in tests.

The properties file contains a single S-expression (usually a map)
with all the properties. In `dev` mode, the file is checked every second
for updates. In non-dev mode, it's checked every minute for changes.

You can access the properties via: `@dragonmark.util.props/info`.
For example, to get the database connection property: `(:db @dragonmark.util.props/info)`.

You can also set up your own atom to watch for changes to the properties:

```
(def db
	(let [a (atom nil)]
	  (dragonmark.util.props/on-prop-change a [:info :db])
	  a))
```

Each time the properties file changes, the `db` atom will be updated.


## License

Dragonmark is dual licensed under the Eclipse Public License,
just like Clojure, and the LGPL 2, your choice.

A side note about licenses... my goal with the license is to
make sure the code is usable in a very wide variety of projects.
Both the EPL and the LGPL have contribute-back clauses. This means
if you make a change to Dragonmark, you have to make your changes
public. But you can use Dragonmark in any project, open or closed.
Also, the dual license is meant to allow Dragonmark to be used in
GPL/AGPL projects... and there are some "issues" between the FSF
and the rest of the world about how open the EPL, the Apache 2, etc.
licenses are. I'm not getting caught in that deal.

(c) 2014 WorldWide Conferencing, LLC

