# commons-room

All signals are sent to receptors using the ss command which takes a single json object as a parameter which is always of this form:

    {"to":<address-integer>, "signal":<signal-name>, "params":<signal-dependent-params>}

Example: to create a new room send the host a "self->host-room" signal, make sure to use your user address as the matrice-address:

    ss {"to":0, "signal":"self->host-room", "params": {"name":"the room", "password":"pass", "matrice-address":3}}

## API


###commons-room
#### signals
    door->enter:
        name: <unique name for the occupant>
        password: <room access password>
        data: <hash of any other data to be associated with that room occupant like full-name, and img-url>
    door->leave: <name>
    stick->request: <name>
    stick->release: <name>
    stick->give: <name>
    matrice->move: <address> <x> <y>
#### scapes
    matrice
> maps occupant address to a single matrice address.  Used for permissions management

    agent
> maps external agent addresses to occupant addresses.  Used for permissions management.

    coord
> maps 2D coordinates onto addresses (occupant or object)

    occupant
> maps names onto occupant addreses