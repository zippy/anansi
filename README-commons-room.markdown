# commons-room

The commons-room is a set of receptors that implement a facilitation architecture for a virtual room.  We are using the commons-room as a prototyping example as we build out the ceptr protocols, but also as a "eat-your-own-dog-food" example, to coordinate participation in mixed physical/virtual meetings.

![commons-room screen-shot](https://skitch.com/zippy314/fy3qp/commons-room-ui-test)

This file documents the signals that can be sent to the commons-room receptor complex.  For documentation on UI access to the commons room see [the commons room UI project](https://github.com/metacurrency/Commons-Room-UI).

## Examples using the telnet access:

To create a new room send the host a "self->host-room" signal, make sure to use your user address as the matrice-address:

    > ss {"to":0, "signal":"self->host-room", "params": {"name":"the room", "password":"pass", "matrice-address":4}}
    {"status":"ok", "result":5}

The result of the self->host-room command is the address of the room, which you use to send it these signals:

To add an occupant to the room, send the room a "door->enter" signal:

    > ss {"to":5, "signal":"door->enter", "params": {"password": "pass" "name":"zippy", "data": {"image-url": "http://images.com/img.jpg"}}}
    {"status":"ok", "result":8}

To move an occupant to a coordinate in the room send the room a "matrice->move":

    > ss {"to":5, "signal":"matrice->move", "params": {"addr":8, "x":100, "y":100}}
    {"status":"ok", "result":{"[100 100]":8}}

To have an occupant leave the room send the room a "door->leave" signal:

    > ss {"to":5, "signal":"door->leave", "params": "zippy"}
    {"status":"ok", "result":null}

To change an occupant's status send the room a "matrice->update-status" signal:

    > ss {"to":5, "signal":"matrice->update-status", "params": {"addr":8 "status":"away"}}
    {"status":"ok", "result":null}

For a complete list of signals that the commons-room can receive, see the API below.
  

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
    occupant->update-data:
        name: <unique name of occupant>
        key: <key to update if updating just a single item in the data hash>
        data: <data value to update>
    matrice->make-agent:
        addr: <address of user to grant grant agency over occupant to>
        occupant: <address of occupant>
    matrice->make-matrice:
        addr: <address of user to grant matrice status to>
    matrice->update-status:
        addr: <address of occupant>
        status: <string value of status state>
    matrice->move: 
        addr: <address of occupant>
        x: <x coord>
        y: <y coord>
    matrice->sit:
        addr: <address of occupant>
        chair: <chair number to assign occupant to>

#### scapes

##### matrice
maps occupant address to a single matrice address.  Used for permissions management

##### agent
maps external agent addresses to occupant addresses.  Used for permissions management.

##### coord
maps 2D coordinates onto addresses (occupant or object)

##### occupant
maps names onto occupant addreses

##### chair
maps chair number onto occupant address

### talking-stick
#### signals
#### scapes
