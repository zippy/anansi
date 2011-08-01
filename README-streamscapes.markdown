# streamscapes
Streamscapes is a set of receptors that implement a communications workflow integration platform.  We are using the streamscapes as a prototyping example as we build out the ceptr protocols, but also as a "eat-your-own-dog-food" example, to coordinate our communications, and solve the massive flow problems we experience with the currently available tools.

This file documents the signals that can be sent to the streamscapes receptor complex.  There is currently no streamscapes UI.

## Examples using the telnet access:

To create a new streamscapes receptor send the host a "self->host-streamscape" signal, make sure to use your user address as the matrice-address:

    > ss {"to":0, "signal":"self->host-streamscape", "params": {"name":"my-ss", "password":"pass", "matrice-address":4, "data": {}}}
    {"status":"ok", "result":5}

The result of the self->host-streamscape command is the address of the streamscape, which you use to send it these signals:

To manually add a droplet into your streamscapes, send it a "matrice->incorporate" signal:

    > ss {"to":5, "signal":"matrice->incorporate", "params": {"from": 1, "to": 2, "aspect": "email", "envelope": {"part1": "grammar address"}, "content": {"part1": "content-data"} }}
    {"status":"ok", "result":8}

For a complete list of signals that streamscapes can receive see the API below.
  

## API

### host
#### signals
    host->streamscape:
        params:
            name: <name for the streamscapes instance>
            password: <access password>
            matrice-address: <address of the user who is the initial matrice>
            data: <a hash of any other data you want to store in this instance>               

### streamscapes
#### signals
    matrice->incorporate:
        from: <ceptr address of the sending streamscapes instance>
        to: <ceptr address of the destination streamscapes instance>
        aspect: <channel through which the droplet entered>
        envelope: <ceptr addresses of the grammars for each part of the droplet>
        contents: <contents of the droplet>

#### scapes

##### matrice
maps agent address to a single matrice address.  Used for permissions management

##### aspect
maps droplet addresses to aspects
