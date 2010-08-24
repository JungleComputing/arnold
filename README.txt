This directory contains an emulator for the BitTorrent protocol.

It implements peer-to-peer file sharing with a file protocol that closely
resembles the BitTorrent protocol, but it uses Ibis IPL send and receive
to exchange these messages, and is therefore not compatible with
the real BitTorrent protocol. It also does not implement some of the
initial handshaking between BitTorrent nodes, nor does it implement
any of the extensions that have been defined.

The emulator was implemented to study two new features for BitTorrent
file sharing: generalized proxying, and piece exchange scheduling
based on money-like credit rather than tit-for-tat.

Proxying means that some nodes in a file-sharing swarm only participate
to help other nodes download the file, not to download the file for
themselves.

The BitTorrent protocol distinguishes between two kinds of peers:
seeders, who already have the file to be shared, and leechers, that
try to download all pieces of the file. As said, this implementation
adds a third kind of peer: proxies.

One of the central issues in the BitTorrent protocol is which piece
requests from other peers should be honoured, and in which order.
The original protocol prescribes a tit-for-tat ranking: a node
should favor nodes that have sent them the most pieces. This
implementation supports tit-for-tat, but also ranking based on
credit: a peer gets credit for providing a piece, and a peer loses
credit by downloading a piece. Requests from peers with high credit
get priority. Note that this differs from tit-for-tat because peers
can get priority based on credit they have earned by providing
pieces to another peer, or even pieces of an entirely different
file. Finally, a static ranking of peers is supported, where
the earlier a peer requests a piece the higher its ranking throughout
the entire download.

Of course tit-for-tat ranking is meaningless for seeders; seeders use
round-robin scheduling instead, favor leechers that are the fastest
in downloading pieces from them (because that's the most efficient
way to spread the file), or favor rarely requested pieces (because
that way knowledge about the file is more evenly spread).


The performance of a file-sharing swarm also depend on the behavior of
peers that have completed their download. When peers immediately
leave the swarm after they have completed their download, the download
performance of the entire swarm will suffer, peers that stay in the swarm
help the swarm.

The simulator provides a PersonalityInterface that allows various
models for the behavior of peers: GreedyPersonality (leave immediately
after download), AltruisticPersonality (stay in the swarm until
every peer has downloaded the file), BigSwarmPersonality (if the
peer starts as a seeder, behave like AltruisticPersonality, else
behave like GreedyPersonality), ImpatientPersonality (stay in the
swarm for a limited time, that depends on the time it took to
download the file). ProxyHelperPersonality implements a specialized
personality that stays in the swarm while the nodes it is helping
are in the swarm (not tested extensively). Finally, TradingPersonality
is designed to stay in the swarm while it is profitable, but this
is not fully implemented.



