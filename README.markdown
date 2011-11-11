# anansi

A reference server for the ceptr platform

## Installation

Anansi is written in [Clojure](http://clojure.org/) so you will need to 
have some version of Java running on your machine.

Install [Leiningen](http://github.com/technomancy/leiningen) if you
haven't already.  Assuming you have a bin directory for your user and
its already in your shell path you can do it like this:

    $ curl -O ~/bin/lein http://github.com/technomancy/leiningen/raw/stable/bin/lein
    $ chmod 755 bin/lein
    $ lein self-install

Clone this repo and let lein install the dependencies:

    $ git clone git://github.com/zippy/anansi.git
    $ cd anansi
    $ lein deps

### Javascript/UI install

Install clojurescript as describe here: https://github.com/clojure/clojurescript/wiki/Quick-Start
Compile clojurscript to js with
    $ cd $ANANSI_HOME/public/ss; rm -rf out; cljsc src > ss.js

## Usage

Anansi provides both direct telnet access to issue commands, and also an HTTP API.

To run the server:

    $ lein run
    Starting web interface on port 8080

You can run the server with options to specify ports and verbose
logging too, like this:

    $ lein run true             ; for verbose logging & default ports
    $ lein run true 3141        ; for verbose logging and custom web port

To access streamscapes UI point browser to http://localhost:8080/ss/index.html

### Telnet access

Once the server is running you can connect to the server by telneting to port 3333

Once connected, you will be asked to enter a user-name:
    
    Welcome to the Anansi server.

    Enter your user name: zippy
    {"status":"ok", "result":{"user-address":34, "host-address":0}}

All server results are returned as a json object that is always of the form:

    {"status:" "ok"|"error"
     "result": <result-value>}

Here are commands available to date:

#### sp

Set a prompt:

    sp ">"
    {"status":"ok", "result":">"}
    >

#### help

Get a list of commands:

    > help
    {"status":"ok",
     "result":
     "exit: Terminate connection with the server\nrl: Request a list of all receptor specification on the server\ngs: Get state\nusers: Get a list of logged in users\nss: Send a signal to a receptor.\nsp: Set prompt\ngc: Get last changes count\nhelp: Show available commands and what they do."}
    

#### users

Get a list of logged in users:

    > users
    {"status":"ok", "result":["zippy"]}

#### ss

Send a signal to a receptor.  The ss command which takes a single json object as a parameter which is always of this form:

    {"to":<address-integer>, "signal":<signal-name>, "params":<signal-dependent-params>}

for example:

    > ss {"to":0, "signal":"self->host-room", "params": {"name": "the room", "password": "pass", "matrice-address":5, "data": {}}}
    {"status":"ok", "result":35}

#### gc

Get the state changes count of a receptor (with no param returns the global changes count):

    > gc {"addr":0}
    {"status":"ok", "result":20}

#### gs

Get the state of a receptor: (you can set full to true for more detailed state info; with no argument returns state of host)

    > gs {"addr":0}
    {"status":"ok",
     "result":
     {"scapes":{"room-scape":{}, "user-scape":{"zippy":4}},
      "receptors":
        {"last-address":4, 
         "4": {"name":"zippy", "type":"user", "address":4}},
      "type":"host",
      "address":1}}

    > gs {"addr":0 "full":true}
    {"status":"ok",
     "result":
     {"scapes-scape-addr":1,
      "receptors": {"last-address":4,
           "4":{"name":"zippy", "type":"user", "address":4},
           "3":{"map":{"zippy":4}, "type":"scape", "address":3},
           "2":{"map":{}, "type":"scape", "address":2},
           "1":{"map":{"room-scape":2, "user-scape":3}, "type":"scape","address":1}},
      "type":"host",
      "address":1}}

#### rl

Get a list of receptors defined on the server:

    > rl
    ...

### HTTP access

As well as listing on port 3333 for telnet connections, Anansi also runs a web-server on port 8080.  You can sent POST requests the "/cmd" url to execute a command.  For example:

    $ curl http://localhost:8080/cmd -d 'username=zippy&cmd=gc&data={"addr":5}'
    {"status":"ok","result":315}

Anansi also directly serves all files (and sub-directories) from the htdocs directory.  So, this is where to put UI code for your receptors.  See [the commons room](https://github.com/metacurrency/Commons-Room-UI)

## Web Development aka How to set up a Browser Repl

1. start the server: cd into a clojurescript checkout, run ./script/repl, and from there do

        (require '[cljs.repl :as repl])
        (require '[cljs.repl.browser :as browser])  ;; require the browser implementation of IJavaScriptEnv
        (def env (browser/repl-env)) ;; create a new environment
        (repl/repl env) ;; start the REPL

2. connect from the client.  make sure that

         (:require [clojure.browser.repl :as repl])

is required in a ns file (currently core.cljs) and that:

          (repl/connect "http://localhost:9000/repl")

is called in the document load.


## Architecture

The ceptr platform consists of a nested hierarchy of receptors, with no necessary top.

[Documentation](https://github.com/zippy/anansi/blob/master/README-ceptr-architecture.markdown)
 

## Commons-room

For prototyping purposes, the anansi server also comes with a set of receptors that model a virtual room with a facilitator.

[Documentation](https://github.com/zippy/anansi/blob/master/README-commons-room.markdown)

## API Documentation

[API](http://zippy.github.com/anansi/)

## Testing

Using midje, install lein-midje as directed at: https://github.com/marick/Midje/wiki/Lein-midje

Run all the unit tests with

    $ lein midje

(Cucumber features are planned...)

## Development

[wiki](https://github.com/zippy/anansi/wiki)

[issue tracking](https://secure.bettermeans.com/projects/1157)


## License

Copyright (C) 2011

Distributed under the Eclipse Public License, the same as Clojure.

## Acknowledgements

Thanks to technomancy for Mire (https://github.com/technomancy/mire)
which got us our start on the server implementation
