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

Get a list of logged in users:

    > users

All signals are sent to receptors using the ss command which takes a single json object as a parameter which is always of this form:

    {"to":<address-integer>, "signal":<signal-name>, "params":<signal-dependent-params>}

for example:

    > ss {"to":0, "signal":"self->host-room", "params": {"name": "the room", :password "pass", "matrice-address":33}}
    
Get a list of receptors defined on the server:

    > rl

Get the state of a receptor: (you can set full to true for more detailed state info)

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

Command results are returned as a json object that is always of the form:

    {"status:" "ok"|"error"
     "result": <result-value>}

## Architecture

The ceptr platform consists of a nested hierarchy of receptors, with no necessary top.

[Documentation](https://github.com/zippy/anansi/blob/master/README-ceptr-architecture.markdown)
 

## Commons-room

For prototyping purposes, the anansi server also comes with a set of receptors that model a virtual room with a facilitator.

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
