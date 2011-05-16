# commons-room

## Examples

To create a new room send the host a "self->host-room" signal, make sure to use your user address as the matrice-address:

    > ss {"to":0, "signal":"self->host-room", "params": {"name":"the room", "password":"pass", "matrice-address":4}}
    {"status":"ok", "result":5}

To add an occupant to the room, send the room a "door->enter" signal:

    > ss {"to":5, "signal":"door->enter", "params": {"password": "pass" "name":"zippy", "data": {"image-url": "http://images.com/img.jpg"}}}
    {"status":"ok", "result":8}

To move an occupant to a coordinate in the room send the room a "matrice->move":

    > ss {"to":5, "signal":"matrice->move", "params": {"addr":8, "x":100, "y":100}}
    {"status":"ok", "result":{"[100 100]":8}}

To have an occupant leave the room send the room a "door->leave" signal:

    > ss {"to":5, "signal":"door->leave", "params": "zippy"}
    {"status":"ok", "result":null}

To change an occupants awareness status send the room a "matrice->update-awareness" signal:

    > ss {"to":5, "signal":"matrice->update-awareness", "params": {"addr":8 "awareness":"asleep"}}
    {"status":"ok", "result":null}

  

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
    matrice->update-awareness:
        addr: <address of occupant>
        awareness: <string value of awareness state>
    matrice->move: 
        addr: <address of occupant>
        x: <x coord>
        y: <y coord>
#### scapes

##### matrice
maps occupant address to a single matrice address.  Used for permissions management

##### agent
maps external agent addresses to occupant addresses.  Used for permissions management.

##### coord
maps 2D coordinates onto addresses (occupant or object)

##### occupant
maps names onto occupant addreses

### talking-stick
#### signals
#### scapes
