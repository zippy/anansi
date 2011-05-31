# ceptr-architecture

> NOTE: the ceptr architecture is in active development.  This whole codebase is a prototyping effort to discover what the architecture of the platform should be.  I.e. the code is ahead of this document and is being written to discover what should be here.  So please don't think you'll read this and figure out how the code should work.  Instead, read this document as a snapshot of what we've been learning as we write the code.

## Overview

Ceptr consists of nested hierarchy of receptors that hold state and send each other signals.  Receptors contain other receptors and also provide an addressing context for the sending of signals to any contained receptors.  Receptors establish the relationships between the receptors they contain by "scaping" them.  Scapes are like indexes, or maps, that locate receptors in some space, thus creating a coherence between them.  Thus receptors can be though of as coherence containers for other receptors.  Because they also create an address space as the base scape for their contained receptors, they can also be thought of as membranes, as any signal that needs to reach a contained receptor, must be directed first to the containing receptor.
Though the "causal" chain that engenders state changes in receptors is strictly limited to the formal signals they receive, the Ceptr platform also provides a non-signal based read-only channel over which receptors can make available aspects of their state.  Architecturally this similar to the way we experience light as a kind of one-way information carrying medium.

## The Anansi Server

The anansi server implements two basic receptor types: host & user.  

A host receptor is responsible for:
 - accepting tcp-ip connections (currently both in the form of a telnet style command based protocol, and also as HTTP requests)
 - instantiating new user receptors for the connections, or attaching a connection to an existing user receptor.
 - instantiating new receptors at the request of other receptors

User receptors implement a model for agency in the ceptr platform.  By convention other receptor types accept signals from user receptors with the understanding that human, and other sources of agency, initiate those signals.

## API

### host
#### signals
    self->host-user:
        name: <unique user name>
> Creates a new user receptor.  Returns the receptor address.

    self->host-room:
        name: <name of the room>
        password: <room access password>
        matrice-address: <address of initial admin>
> Creates a new commons room receptor.  Returns the receptor address.
#### scapes
    room-scape
> maps names to addresses
    user-scape
> maps names to addresses

### user
#### signals
    self->connect [stream]
> Connects the user to a stream.

    self->disconnect
> Disconnects the user from a stream.

#### scapes

