package com.hedera.hashgraph.sdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Response from the mirror node containing the estimated fees for a transaction.
 */
public final class FeeEstimateResponse {
    /** The estimation mode used by the mirror node. */
    public final FeeEstimateMode mode;
    /** The network fee component. */
    public final NetworkFee network;
    /** The node fee component. */
    public final FeeEstimate node;
    /** The service fee component. */
    public final FeeEstimate service;
    /** Notes returned by the estimator. */
    public final List<String> notes;
    /** Total estimated fee in tinycents. */
    public final long total;

    FeeEstimateResponse(
        FeeEstimateMode mode, NetworkFee network, FeeEstimate node, FeeEstimate service, List<String> notes, long total) {
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
        this.network = Objects.requireNonNull(network, "network must not be null");
        this.node = Objects.requireNonNull(node, "node must not be null");
        this.service = Objects.requireNonNull(service, "service must not be null");
        this.notes = Collections.unmodifiableList(new ArrayList<>(notes));
        this.total = total;
    }

    static FeeEstimateResponse fromProtobuf(com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateResponse response) {
        var mode = FeeEstimateMode.fromProtobuf(response.getMode());
        var network = NetworkFee.fromProtobuf(response.getNetwork());
        var node = FeeEstimate.fromProtobuf(response.getNode());
        var service = FeeEstimate.fromProtobuf(response.getService());
        return new FeeEstimateResponse(mode, network, node, service, response.getNotesList(), response.getTotal());
    }

    FeeEstimateResponse aggregate(FeeEstimateResponse other) {
        if (network.multiplier != other.network.multiplier) {
            throw new IllegalArgumentException("Cannot aggregate fee estimates with different network multipliers");
        }
        if (mode != other.mode) {
            throw new IllegalArgumentException("Cannot aggregate fee estimates calculated with different modes");
        }

        var aggregatedNode = node.aggregate(other.node);
        var aggregatedService = service.aggregate(other.service);

        long aggregatedNodeSubtotal = aggregatedNode.subtotal();
        long aggregatedServiceSubtotal = aggregatedService.subtotal();
        long aggregatedNetworkSubtotal = Math.addExact(network.subtotal, other.network.subtotal);
        long expectedNetworkSubtotal = Math.multiplyExact(network.multiplier, aggregatedNodeSubtotal);
        if (aggregatedNetworkSubtotal != expectedNetworkSubtotal) {
            aggregatedNetworkSubtotal = expectedNetworkSubtotal;
        }

        long total = Math.addExact(Math.addExact(aggregatedNodeSubtotal, aggregatedServiceSubtotal), aggregatedNetworkSubtotal);

        Set<String> combinedNotes = new LinkedHashSet<>(notes);
        combinedNotes.addAll(other.notes);

        return new FeeEstimateResponse(
            mode,
            new NetworkFee(network.multiplier, aggregatedNetworkSubtotal),
            aggregatedNode,
            aggregatedService,
            new ArrayList<>(combinedNotes),
            total);
    }

    @Override
    public String toString() {
        return "FeeEstimateResponse{"
            + "mode=" + mode
            + ", network=" + network
            + ", node=" + node
            + ", service=" + service
            + ", notes=" + notes
            + ", total=" + total
            + '}';
    }
}
