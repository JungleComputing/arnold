package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

class RankedPeerList implements Iterable<PeerInfo> {
    private final ArrayList<PeerInfo> list = new ArrayList<PeerInfo>();
    private PeerRanker ranker;

    RankedPeerList(final PeerRanker ranker) {
        this.ranker = ranker;
    }

    int size() {
        return list.size();
    }

    void add(final PeerInfo p) {
        final int pos = Collections.binarySearch(list, p, ranker);
        if (pos >= 0) {
            list.add(pos, p);
        } else {
            list.add(-pos - 1, p);
        }
    }

    void updateRanking(final int ix) {
        final PeerInfo p = list.remove(ix);
        add(p);
    }

    void updateRanking(final PeerInfo p) {
        final int ix = list.indexOf(p);
        if (ix < 0) {
            Globals.log.reportInternalError("PeerInfo " + p + " not found");
            return;
        }
        updateRanking(ix);
    }

    PeerInfo remove(final int ix) {
        return list.remove(ix);
    }

    boolean remove(final PeerInfo p) {
        return list.remove(p);
    }

    PeerInfo get(final int ix) {
        return list.get(ix);
    }

    boolean isEmpty() {
        return list.isEmpty();
    }

    List<PeerInfo> getList() {
        return list;
    }

    @Override
    public Iterator<PeerInfo> iterator() {
        return list.iterator();
    }

    private void resort() {
        Collections.sort(list, ranker);
    }

    void setRanker(final PeerRanker r) {
        ranker = r;
        resort();
    }

    int findPeer(final IbisIdentifier peer, final boolean allowDeleted) {
        for (int ix = 0; ix < list.size(); ix++) {
            final PeerInfo p = list.get(ix);
            if (peer.equals(p.peer) && (allowDeleted || !p.isDeleted())) {
                return ix;
            }
        }
        return -1;
    }

}
