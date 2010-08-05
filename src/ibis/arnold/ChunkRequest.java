package ibis.arnold;

import ibis.ipl.IbisIdentifier;

class ChunkRequest {
    final Chunk chunk;
    final IbisIdentifier peer;
    final long requestMoment;

    protected ChunkRequest(final Chunk chunk, final IbisIdentifier peer) {
        this.chunk = chunk;
        this.peer = peer;
        this.requestMoment = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "ReceviedChunkRequest[chunk=" + chunk + ",peer=" + peer + "]";
    }
}
