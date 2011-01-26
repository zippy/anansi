# anansi

A reference server for the ceptr platform

## Installation

Install [Leiningen](http://github.com/technomancy/leiningen) if you
haven't already.  Assuming you have a bin directory for your user and
its already in you shell path you can do it like this:

    $ curl -O ~/bin/lein http://github.com/technomancy/leiningen/raw/stable/bin/lein
    $ chmod 755 bin/lein
    $ lein self-install

Clone this repo and let lein install the dependencies:

    $ git clone git://github.com/zippy/anansi.git
    $ lein deps

Once it finishes, you should be able to do "lein run" to launch the anansi server. Then
user can connect by telnetting to port 3333.

## Usage

FIXME

## Documentation

http://zippy.github.com/anansi/

## License

Copyright (C) 2011

Distributed under the Eclipse Public License, the same as Clojure.

## Acknowledgements

This code borrows heavily from Mire (https://github.com/technomancy/mire) for it's server implementation
