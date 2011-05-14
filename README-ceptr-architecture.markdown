# ceptr-architecture

> NOTE: the ceptr architecture is in active development.  This whole codebase is a prototyping effort to discover what the architecture of the platform should be.  I.e. the code is ahead of this document and is being written to discover what should be here.  So please don't think you'll read this and figure out how the code should work.  Instead, read this document as a snapshot of what we've been learning as we write the code.

## overview

Ceptr consists of receptors that send each other signals.  Receptors contain other receptors and also provide an addressing context for the sending of signals to any contained receptors.  Receptors establish the relationships between the receptors they contain by "scaping" them.  Scapes are like indexes, or maps, that locate receptors in some space, thus creating a coherence between them.  Thus receptors can be though of as coherence containers for other receptors.  Because they also create an address space as the base scape for their contained receptors, they can also be thought of as membranes, as any signal that needs to reach a contained receptor, must be directed first to the containing receptor.

## anansi

The anansi server implements two basic receptor types: host & user.  

A host receptor is responsible for:
 - accepting tcp-ip connections and instantiating new user receptors for the connections, or attaching a connection to an existing user receptor.
 - instantiating new receptors at the request of other receptors

User receptors implement a model for agency in the ceptr platform.  By convention other receptor types accept signals from user receptrs with the understanding that human, and other sources of agency, initiate those signals.

## api

### host
    self->host-room [name password matrice-address]
    self->host-user [name]
### user
    self->disconnect
    self->connect [stream]