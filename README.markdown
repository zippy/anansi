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
    $ cd anansi
    $ lein deps

## Usage

To run the server:

    $ lein run

Then you can connect to the server by telneting to port 3333

Once connected, enter your user-name and then when the user has been
attached you can enter the following commands:

For a list of commands:
    > help

Send a signal:
    ss <to, signal, params> encoded as json  
for example:
    > ss {"to":0, "signal":"self->host-room", "params": {"name": "the room", :password "pass", "matrice-address":33}}
    
Get a list of receptors defined on the server:
    > rl
    

## Commons-room

For prototyping purposes, the anansi server comes with a set of receptors that model a virtual room with a facilitator.

[Documentation](https://github.com/zippy/anansi/blob/master/README-commons-room.markdown)

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