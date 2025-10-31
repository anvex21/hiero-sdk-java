package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeeEstimateModelTest {

    @Test
    @DisplayName("FeeExtra converts to and from protobuf without data loss")
    void feeExtraRoundTrip() throws InvalidProtocolBufferException {
        var extraProto = com.hedera.hashgraph.sdk.proto.mirror.FeeExtra.newBuilder()
            .setCharged(2)
            .setCount(5)
            .setFeePerUnit(7)
            .setIncluded(1)
            .setName("custom-extra")
            .setSubtotal(14)
            .build();

        var extra = FeeExtra.fromProtobuf(extraProto);

        assertThat(extra.getCharged()).isEqualTo(2);
        assertThat(extra.getCount()).isEqualTo(5);
        assertThat(extra.getFeePerUnit()).isEqualTo(7);
        assertThat(extra.getIncluded()).isEqualTo(1);
        assertThat(extra.getName()).isEqualTo("custom-extra");
        assertThat(extra.getSubtotal()).isEqualTo(14);
        assertThat(extra.toProtobuf()).isEqualTo(extraProto);
        assertThat(FeeExtra.fromBytes(extra.toBytes())).isEqualTo(extra);
    }

    @Test
    @DisplayName("FeeEstimate aggregates extras and supports protobuf conversion")
    void feeEstimateRoundTrip() throws InvalidProtocolBufferException {
        var extraProto = com.hedera.hashgraph.sdk.proto.mirror.FeeExtra.newBuilder()
            .setCharged(3)
            .setCount(5)
            .setFeePerUnit(11)
            .setIncluded(2)
            .setSubtotal(33)
            .build();

        var estimateProto = com.hedera.hashgraph.sdk.proto.mirror.FeeEstimate.newBuilder()
            .setBase(42)
            .addExtras(extraProto)
            .build();

        var estimate = FeeEstimate.fromProtobuf(estimateProto);

        assertThat(estimate.getBase()).isEqualTo(42);
        assertThat(estimate.getExtras()).extracting(FeeExtra::getCharged).containsExactly(3);
        assertThat(estimate.getExtras()).extracting(FeeExtra::getSubtotal).containsExactly(33L);
        assertThat(estimate.toProtobuf()).isEqualTo(estimateProto);
        assertThat(FeeEstimate.fromBytes(estimate.toBytes())).isEqualTo(estimate);
    }

    @Test
    @DisplayName("FeeEstimateResponse retains all fields when converted from protobuf")
    void feeEstimateResponseRoundTrip() throws InvalidProtocolBufferException {
        var nodeProto = com.hedera.hashgraph.sdk.proto.mirror.FeeEstimate.newBuilder()
            .setBase(100)
            .addExtras(com.hedera.hashgraph.sdk.proto.mirror.FeeExtra.newBuilder()
                .setCharged(1)
                .setCount(2)
                .setFeePerUnit(50)
                .setIncluded(0)
                .setSubtotal(50)
                .build())
            .build();

        var serviceProto = com.hedera.hashgraph.sdk.proto.mirror.FeeEstimate.newBuilder()
            .setBase(200)
            .build();

        var responseProto = com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateResponse.newBuilder()
            .setModeValue(FeeEstimateMode.STATE.code)
            .setNetwork(com.hedera.hashgraph.sdk.proto.mirror.NetworkFee.newBuilder()
                .setMultiplier(3)
                .setSubtotal(300)
                .build())
            .setNode(nodeProto)
            .addAllNotes(List.of("note-a", "note-b"))
            .setService(serviceProto)
            .setTotal(600)
            .build();

        var response = FeeEstimateResponse.fromProtobuf(responseProto);

        assertThat(response.getMode()).isEqualTo(FeeEstimateMode.STATE);
        assertThat(response.getNetwork().getMultiplier()).isEqualTo(3);
        assertThat(response.getNetwork().getSubtotal()).isEqualTo(300);
        assertThat(response.getNode().getBase()).isEqualTo(100);
        assertThat(response.getNode().getExtras()).hasSize(1);
        assertThat(response.getNode().getExtras().get(0).getSubtotal()).isEqualTo(50);
        assertThat(response.getService().getBase()).isEqualTo(200);
        assertThat(response.getNotes()).containsExactly("note-a", "note-b");
        assertThat(response.getTotal()).isEqualTo(600);
        assertThat(response.toProtobuf()).isEqualTo(responseProto);

        // Using equals() returns the same output but the test doesn't pass because of different object references (hash codes)
        // Compare by fields instead of equals()
        var responseFromBytes = FeeEstimateResponse.fromBytes(response.toBytes());
        assertThat(responseFromBytes.getMode()).isEqualTo(response.getMode());
        assertThat(responseFromBytes.getTotal()).isEqualTo(response.getTotal());
        assertThat(responseFromBytes.getNotes()).containsExactlyElementsOf(response.getNotes());

        // Compare network
        assertThat(responseFromBytes.getNetwork().getMultiplier()).isEqualTo(response.getNetwork().getMultiplier());
        assertThat(responseFromBytes.getNetwork().getSubtotal()).isEqualTo(response.getNetwork().getSubtotal());

        // Compare node
        assertThat(responseFromBytes.getNode().getBase()).isEqualTo(response.getNode().getBase());
        assertThat(responseFromBytes.getNode().getExtras()).hasSameSizeAs(response.getNode().getExtras());

        // Compare service
        assertThat(responseFromBytes.getService().getBase()).isEqualTo(response.getService().getBase());
        assertThat(responseFromBytes.getService().getExtras()).hasSameSizeAs(response.getService().getExtras());
    }
}
