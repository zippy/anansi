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

## Usage

To run the server:

    $ lein run

Then you can connect to the server by telneting to port 3333

Once connected, enter your user-name and then when the user has been
attached--

For a list of commands:
    > help

Sending a signal:
    > send {:to "some-address:some-aspect" :body {:some-key "some value"}}

For testing purposes the server comes with a room receptor that scapes
people into a circle and allows passing object receptors between
them. For example:

    > send {:to "server:conjure", :body {:name "room",:type "Room"}}
    created
    > send {:to "room:enter", :body {:person {:name "Art"}}}
    entered as art
    > send {:to "room:enter", :body {:person {:name "Eric"}}}
    entered as eric
    > send {:to "room:conjure", :body {:name "stick",:type "Object"}}
    {:seat {0 art, 1 eric}, :angle {0 art, 180 eric}, :coords {[0 0] stick, [0 -500] art, [0 500] eric}, :holding {}}
    > send {:to "room:pass-object", :body {:object "stick",:to "art_brock"}}
    {:seat {0 art, 1 eric}, :angle {0 art, 180 eric}, :coords {[0 -490] stick, [0 -500] art, [0 500] eric}, :holding {art stick}}

## Documentation

[API](http://zippy.github.com/anansi/)

## Testing

Run all the unit tests with 

    $ lein test

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
