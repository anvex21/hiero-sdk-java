package com.hedera.hashgraph.sdk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fee estimate for a node or service component.
 */
public final class FeeEstimate {
    /** The base fee in tinycents. */
    public final long base;
    /** Extra fee components. */
    public final List<FeeExtra> extras;

    FeeEstimate(long base, List<FeeExtra> extras) {
        this.base = base;
        this.extras = List.copyOf(extras);
    }

    static FeeEstimate fromProtobuf(com.hedera.hashgraph.sdk.proto.mirror.FeeEstimate proto) {
        var extras = new ArrayList<FeeExtra>(proto.getExtrasCount());
        for (var extra : proto.getExtrasList()) {
            extras.add(FeeExtra.fromProtobuf(extra));
        }
        return new FeeEstimate(proto.getBase(), extras);
    }

    long subtotal() {
        long subtotal = base;
        for (var extra : extras) {
            subtotal = Math.addExact(subtotal, extra.subtotal);
        }
        return subtotal;
    }

    FeeEstimate aggregate(FeeEstimate other) {
        long aggregatedBase = Math.addExact(base, other.base);
        Map<String, FeeExtra> aggregatedExtras = new LinkedHashMap<>();

        for (var extra : extras) {
            aggregatedExtras.put(extra.name, extra);
        }

        for (var extra : other.extras) {
            aggregatedExtras.merge(extra.name, extra, FeeExtra::aggregate);
        }

        return new FeeEstimate(aggregatedBase, new ArrayList<>(aggregatedExtras.values()));
    }

    @Override
    public String toString() {
        return "FeeEstimate{" + "base=" + base + ", extras=" + extras + '}';
    }
}
