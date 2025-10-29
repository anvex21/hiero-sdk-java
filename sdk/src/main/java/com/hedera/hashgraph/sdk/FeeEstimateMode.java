package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.mirror.EstimateMode;

/**
 * Determines whether the fee estimate should consider the network state or only the intrinsic
 * properties of the transaction payload.
 */
public enum FeeEstimateMode {
    /** Estimate using the mirror node's best understanding of state. */
    STATE,

    /** Estimate purely from the transaction payload, ignoring state dependent costs. */
    INTRINSIC;

    static FeeEstimateMode fromProtobuf(EstimateMode mode) {
        return switch (mode) {
            case INTRINSIC -> INTRINSIC;
            case UNRECOGNIZED, STATE -> STATE;
        };
    }

    EstimateMode toProtobuf() {
        return switch (this) {
            case STATE -> EstimateMode.STATE;
            case INTRINSIC -> EstimateMode.INTRINSIC;
        };
    }
}
