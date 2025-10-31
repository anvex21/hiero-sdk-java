package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.proto.mirror.NetworkServiceGrpc;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeeEstimateQueryMockTest {

    private static final com.hedera.hashgraph.sdk.proto.Transaction DUMMY_TRANSACTION =
        com.hedera.hashgraph.sdk.proto.Transaction.newBuilder()
            .setSignedTransactionBytes(ByteString.copyFromUtf8("dummy"))
            .build();

    private Client client;
    private FeeEstimateServiceStub feeEstimateServiceStub;
    private Server server;
    private FeeEstimateQuery query;

    @BeforeEach
    void setUp() throws Exception {
        client = Client.forNetwork(Collections.emptyMap());
        client.setMirrorNetwork(Collections.singletonList("in-process:test"));

        feeEstimateServiceStub = new FeeEstimateServiceStub();
        server = InProcessServerBuilder.forName("test")
            .addService(feeEstimateServiceStub)
            .directExecutor()
            .build()
            .start();

        query = new FeeEstimateQuery();
    }

    @AfterEach
    void tearDown() throws Exception {
        feeEstimateServiceStub.verify();
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.shutdown();
            server.awaitTermination();
        }
    }

    @Test
    @DisplayName("Given no mode is provided, when a fee estimate is requested, then STATE is used by default")
    void executesWithDefaultStateMode() {
        var expectedRequest = com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateQuery.newBuilder()
            .setModeValue(FeeEstimateMode.STATE.code)
            .setTransaction(DUMMY_TRANSACTION)
            .build();
        var successResponse = newSuccessResponse(FeeEstimateMode.STATE, 2, 20, 30);

        feeEstimateServiceStub.enqueue(expectedRequest, successResponse);

        var response = query.setTransaction(DUMMY_TRANSACTION).execute(client);

        assertThat(response.getMode()).isEqualTo(FeeEstimateMode.STATE);
        assertThat(response.getNetwork().getMultiplier()).isEqualTo(2);
        assertThat(response.getNetwork().getSubtotal()).isEqualTo(40);
        assertThat(response.getNode().getBase()).isEqualTo(20);
        assertThat(response.getService().getBase()).isEqualTo(30);
        assertThat(response.getTotal()).isEqualTo(90);
    }

    @Test
    @DisplayName("Given intrinsic mode is set, when a fee estimate is requested, then the request uses INTRINSIC mode")
    void executesWithIntrinsicMode() {
        var expectedRequest = com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateQuery.newBuilder()
            .setModeValue(FeeEstimateMode.INTRINSIC.code)
            .setTransaction(DUMMY_TRANSACTION)
            .build();
        var successResponse = newSuccessResponse(FeeEstimateMode.INTRINSIC, 3, 5, 7);

        feeEstimateServiceStub.enqueue(expectedRequest, successResponse);

        var response = query
            .setMode(FeeEstimateMode.INTRINSIC)
            .setTransaction(DUMMY_TRANSACTION)
            .execute(client);

        assertThat(response.getMode()).isEqualTo(FeeEstimateMode.INTRINSIC);
        assertThat(response.getNetwork().getMultiplier()).isEqualTo(3);
        assertThat(response.getTotal()).isEqualTo(27);
    }

    @Test
    @DisplayName("Given a transient transport error, when a fee estimate is requested, then the query retries and succeeds")
    void retriesOnTransientErrors() {
        query.setTransaction(DUMMY_TRANSACTION)
            .setMaxAttempts(3)
            .setMaxBackoff(Duration.ofMillis(500));

        var expectedRequest = com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateQuery.newBuilder()
            .setModeValue(FeeEstimateMode.STATE.code)
            .setTransaction(DUMMY_TRANSACTION)
            .build();

        feeEstimateServiceStub.enqueueError(
            expectedRequest, Status.UNAVAILABLE.withDescription("transient").asRuntimeException());
        feeEstimateServiceStub.enqueue(expectedRequest, newSuccessResponse(FeeEstimateMode.STATE, 2, 6, 8));

        var response = query.execute(client);

        assertThat(response.getMode()).isEqualTo(FeeEstimateMode.STATE);
        assertThat(response.getTotal()).isEqualTo(26);
        assertThat(feeEstimateServiceStub.requestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Given an invalid argument error, when a fee estimate is requested, then the query does not retry")
    void doesNotRetryOnInvalidArgument() {
        query.setTransaction(DUMMY_TRANSACTION).setMaxAttempts(3);

        var expectedRequest = com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateQuery.newBuilder()
            .setModeValue(FeeEstimateMode.STATE.code)
            .setTransaction(DUMMY_TRANSACTION)
            .build();

        feeEstimateServiceStub.enqueueError(
            expectedRequest, Status.INVALID_ARGUMENT.withDescription("bad txn").asRuntimeException());

        assertThatThrownBy(() -> query.execute(client))
            .isInstanceOf(StatusRuntimeException.class)
            .hasMessageContaining("INVALID_ARGUMENT");
        assertThat(feeEstimateServiceStub.requestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Given async execution, when a fee estimate is requested, then the future completes with the response")
    void executesAsync() throws Exception {
        var expectedRequest = com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateQuery.newBuilder()
            .setModeValue(FeeEstimateMode.STATE.code)
            .setTransaction(DUMMY_TRANSACTION)
            .build();
        var successResponse = newSuccessResponse(FeeEstimateMode.STATE, 4, 8, 20);

        feeEstimateServiceStub.enqueue(expectedRequest, successResponse);

        var response = query.setTransaction(DUMMY_TRANSACTION).executeAsync(client).get();

        assertThat(response.getMode()).isEqualTo(FeeEstimateMode.STATE);
        assertThat(response.getTotal()).isEqualTo(60);
    }

    private static com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateResponse newSuccessResponse(
        FeeEstimateMode mode, int networkMultiplier, long nodeBase, long serviceBase) {
        long networkSubtotal = nodeBase * networkMultiplier;
        long total = networkSubtotal + nodeBase + serviceBase;
        return com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateResponse.newBuilder()
            .setModeValue(mode.code)
            .setNetwork(com.hedera.hashgraph.sdk.proto.mirror.NetworkFee.newBuilder()
                .setMultiplier(networkMultiplier)
                .setSubtotal(networkSubtotal)
                .build())
            .setNode(com.hedera.hashgraph.sdk.proto.mirror.FeeEstimate.newBuilder()
                .setBase(nodeBase)
                .build())
            .setService(com.hedera.hashgraph.sdk.proto.mirror.FeeEstimate.newBuilder()
                .setBase(serviceBase)
                .build())
            .setTotal(total)
            .build();
    }

    private static class FeeEstimateServiceStub extends NetworkServiceGrpc.NetworkServiceImplBase {
        private final Queue<com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateQuery> expectedRequests =
            new ArrayDeque<>();
        private final Queue<Object> responses = new ArrayDeque<>();
        private int observedRequests = 0;

        void enqueue(
            com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateQuery request,
            com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateResponse response) {
            expectedRequests.add(request);
            responses.add(response);
        }

        void enqueueError(
            com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateQuery request,
            StatusRuntimeException error) {
            expectedRequests.add(request);
            responses.add(error);
        }

        int requestCount() {
            return observedRequests;
        }

        void verify() {
            assertThat(expectedRequests).isEmpty();
            assertThat(responses).isEmpty();
        }

        @Override
        public void getFeeEstimate(
            com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateQuery request,
            StreamObserver<com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateResponse> responseObserver) {
            observedRequests++;
            var expected = expectedRequests.poll();
            assertThat(expected)
                .as("expected request to be queued before invoking getFeeEstimate")
                .isNotNull();
            assertThat(request).isEqualTo(expected);

            var response = responses.poll();
            assertThat(response)
                .as("response or error should be queued before invoking getFeeEstimate")
                .isNotNull();

            if (response instanceof StatusRuntimeException error) {
                responseObserver.onError(error);
                return;
            }

            responseObserver.onNext((com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateResponse) response);
            responseObserver.onCompleted();
        }
    }
}
