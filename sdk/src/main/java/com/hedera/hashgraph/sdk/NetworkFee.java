package com.hedera.hashgraph.sdk;

/**
 * Network fee component of the estimate.
 */
public final class NetworkFee {
    /** Multiplier applied to the node subtotal. */
    public final long multiplier;
    /** Subtotal for the network component, in tinycents. */
    public final long subtotal;

    NetworkFee(long multiplier, long subtotal) {
        this.multiplier = multiplier;
        this.subtotal = subtotal;
    }

    static NetworkFee fromProtobuf(com.hedera.hashgraph.sdk.proto.mirror.NetworkFee proto) {
        return new NetworkFee(Integer.toUnsignedLong(proto.getMultiplier()), proto.getSubtotal());
    }

    com.hedera.hashgraph.sdk.proto.mirror.NetworkFee toProtobuf() {
        return com.hedera.hashgraph.sdk.proto.mirror.NetworkFee.newBuilder()
            .setMultiplier((int) multiplier)
            .setSubtotal(subtotal)
            .build();
    }

    @Override
    public String toString() {
        return "NetworkFee{" + "multiplier=" + multiplier + ", subtotal=" + subtotal + '}';
    }
}
