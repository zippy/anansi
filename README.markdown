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
   > send {:to "server.users" :from "user.?"}

## Documentation

[API](http://zippy.github.com/anansi/)

## Testing

Run all the unit tests with 

    $ lein test

(Cucumber features are planned...)

## Development

[wiki](https://github.com/zippy/anansi/wiki)
[issue tracking](https://www.pivotaltracker.com/projects/219347)


## License

Copyright (C) 2011

Distributed under the Eclipse Public License, the same as Clojure.

## Acknowledgements

This code borrows heavily from Mire (https://github.com/technomancy/mire) for it's server implementation
