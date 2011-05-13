# commons-room

All signals are sent to receptors using the ss command which takes a single json object as a parameter which is always of this form:

    {"to":<address-integer>, "signal":<signal-name>, "params":<signal-dependent-params>}

Example: to create a new room send the host a "self->host-room" signal, make sure to use your user address as the matrice-address:

    ss {"to":0, "signal":"self->host-room", "params": {"name":"the room", "password":"pass", "matrice-address":3}}

---

Here is a list of the receptors available and the signals you can send to them:

##host
    self->host-room:
        name: <name of the room>
        password: <room access password>
        matrice-address: <address of initial admin>
    

##commons-room
    door->enter:
        name: <unique name for the occupant>
        password: <room access password>
        data: <hash of any other data to be associated with that room occupant like full-name, and img-url>
    door->leave: <name>
