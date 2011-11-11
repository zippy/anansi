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

### HTTP access

The main access to anansi is through an http api (though telnet access
has also been implemented).  Anansi
runs a web-server on port 8080.  You can send POST requests the "/api"
url to execute a command.  All request are JSON encoded hashes, for example:

    $ curl http://localhost:8080/api -d '{"cmd":"get-state","params":{"receptor":13}}'
    {"status":"ok","result" ... }

Anansi also directly serves all files (and sub-directories) from the
htdocs directory.  So, this is where to put UI code for your
receptors.

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
 

## Streamscapes

As a test case for building out the ceptr platform, the anansi server
includes a communications integration application called Streamscapes:

[Documentation](https://github.com/zippy/anansi/blob/master/README-streamscapes.markdown)

## API Documentation

[API](http://zippy.github.com/anansi/)

## Testing

Using midje, install lein-midje as directed at: https://github.com/marick/Midje/wiki/Lein-midje

Run all the unit tests with

    $ lein midje

(Cucumber features are planned...)

## Development

[issue tracking](https://github.com/zippy/anansi/issue)

[wiki](https://github.com/zippy/anansi/wiki)

## License

Copyright (C) 2011, The MetaCurrencyProject (Eric Harris-Braun, et. al.)

Distributed under the Eclipse Public License, the same as Clojure.

## Acknowledgements

Thanks to ztellman for aleph, technomancy for swank-clojure, and all
the good folks on the #clojure freenode channel and mailing-list for
being way helpful.
