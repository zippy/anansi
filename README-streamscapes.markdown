# streamscapes
Streamscapes is a set of receptors that implement a communications workflow integration platform.
We are using streamscapes as a prototyping example as we build out the ceptr protocols, but also as a "eat-your-own-dog-food" example,
to coordinate our communications, and solve the massive flow problems we experience with the currently available tools.

This file documents the signals that defined in the streamscapes receptor complex.

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
            matrice-address: <ceptr address of the user receptor who is the initial matrice>
            data: <a hash of any other data you want to store in this instance>               

### streamscapes
#### signals
    channel->incorporate:
        description:
            a signal for contained channel receptors to send to their parent streamscapes receptor when they have a droplet that should be created
        params:
            from: <ceptr address of the identity receptor that is the source of the droplet>
            to: <ceptr address of the identity receptor that is the destination for the droplet>
            aspect: <channel through which the droplet was received>
            envelope: <ceptr addresses of the grammars for each part of the droplet>
            contents: <contents of the droplet>

    matrice->incorporate:
        description:
            a signal for matrices of a streamscapes instance to send to the instance to create a droplet.
        params:
            from: <ceptr address of the identity receptor that is the source of the droplet>
            to: <ceptr address of the identity receptor that is the destination for the droplet>
            aspect: <channel through which the droplet should be delivered>
            envelope: <ceptr addresses of the grammars for each part of the droplet>
            contents: <contents of the droplet>

    matrice->identify:
        description:
            a signal which creates or returns identity(ies) based on identifiers and attributes.
        params:
            :identifiers <key value pairs of names and values that uniquely identify an idenity, i.e. ssn:123-45-6789>
            :attributes <key value pairs of names and values that non-uniquely are associated with an identiy, i.e. eye-color:blue>
    
    matrice->make-channel
        description:
            a signal used to instantiate a new channel receptor in the streamscapes instance
        params:
            :name: <channel name>
            receptors: <key value pairs that define a list of containing bridge receptors>
                <key> -> <receptor-type>
                <value> -> params
                    role: <receiver|deliverer|controller>
                    signal: <signal to use to effect the action of this receptor, if needed>
                    params: <any parameters necessary to for instantiating this sub-receptor>
        example:
        > ss {"to":0, "signal":"matrice->make-channel", "params": 
                {"name":"email-stream", 
                 "receptors": 
                    {"email-bridge-in": {"role":"receiver"
                                         "params":{"host":"mail.example.com", "account": "someuser", "password":"pass", "protocol":"pop3"}}}
                    {"email-bridge-in": {"role":"deliverer", "signal":"channel->deliver",
                                         "params":{"host":"mail.google.com", "account": "someuser", "password":"pass", "protocol":"smtps", "port":25}}}}}

    streamscapes->receive
        description:
            a signal for streamscapes instances to send each other streamscapes droplets
        params:
            aspect: <name of channel to which this signal is directed>
            from: <ceptr address of the streamscapes instance this signal is from>
            to: <ceptr address of the streamscapes instance this signal is to>
            envelope: <ceptr addresses of the grammars for each part of the contents>
            contents: <contents of the droplet>
            
#### scapes
##### matrice
maps agent address to a single matrice address.  Used for permissions management

##### aspect
maps droplet addresses to aspects (i.e. channel-names).  This is useful to identify which channel a droplet arrived through.

##### id
maps droplet id to droplet address.  This useful for doing things like checking if a droplet has already been created with this id, i.e. when a channel bridge is gets duplicate entries (for example when scanning emails from a POP server)

### channels
Each streamscapes instance creates channels through which flow the "droplets" that are the basic information streamscapes information packages.  Currently there are bridge receptors defined that allow instantiation of channels for:

#### email sending/receiving
#### streamscapes to streamscapes signaling
#### connection to an IRC server to send and receive messages on an IRC channel