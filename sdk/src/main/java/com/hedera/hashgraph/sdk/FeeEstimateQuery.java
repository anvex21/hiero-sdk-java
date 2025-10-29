package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.mirror.NetworkServiceGrpc;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.Deadline;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCalls;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Query the mirror network for an estimated fee for a transaction.
 */
public final class FeeEstimateQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeeEstimateQuery.class);

    @Nullable
    private FeeEstimateMode mode = null;

    @Nullable
    private Transaction<?> transaction = null;

    private int maxAttempts = Client.DEFAULT_MAX_ATTEMPTS;

    private Duration maxBackoff = Client.DEFAULT_MAX_BACKOFF;

    /**
     * Assign the estimation mode.
     *
     * @param mode the desired estimation mode
     * @return {@code this}
     */
    public FeeEstimateQuery setMode(@Nullable FeeEstimateMode mode) {
        this.mode = mode;
        return this;
    }

    /**
     * Extract the estimation mode.
     *
     * @return the mode or {@code null} if none has been set
     */
    @Nullable
    public FeeEstimateMode getMode() {
        return mode;
    }

    /**
     * Assign the transaction to be estimated.
     *
     * @param transaction the transaction to estimate
     * @return {@code this}
     */
    public FeeEstimateQuery setTransaction(Transaction<?> transaction) {
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        return this;
    }

    /**
     * Extract the transaction currently set for estimation.
     *
     * @return the transaction or {@code null} if none has been set
     */
    @Nullable
    public Transaction<?> getTransaction() {
        return transaction;
    }

    /**
     * Extract the maximum number of attempts used for retrying transient errors.
     *
     * @return maximum attempts
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Assign the maximum number of attempts used for retrying transient errors.
     *
     * @param maxAttempts maximum attempts
     * @return {@code this}
     */
    public FeeEstimateQuery setMaxAttempts(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        this.maxAttempts = maxAttempts;
        return this;
    }

    /**
     * Assign the maximum backoff duration used when retrying transient errors.
     *
     * @param maxBackoff the maximum backoff duration
     * @return {@code this}
     */
    public FeeEstimateQuery setMaxBackoff(Duration maxBackoff) {
        Objects.requireNonNull(maxBackoff, "maxBackoff must not be null");
        if (maxBackoff.toMillis() < Client.DEFAULT_MIN_BACKOFF.toMillis()) {
            throw new IllegalArgumentException("maxBackoff must be at least " + Client.DEFAULT_MIN_BACKOFF.toMillis() + " ms");
        }
        this.maxBackoff = maxBackoff;
        return this;
    }

    /**
     * Execute the query using the client's default request timeout.
     *
     * @param client the client to use
     * @return the aggregated fee estimate response
     */
    public FeeEstimateResponse execute(Client client) {
        return execute(client, client.getRequestTimeout());
    }

    /**
     * Execute the query with a user provided timeout.
     *
     * @param client  the client to use
     * @param timeout the timeout
     * @return the aggregated fee estimate response
     */
    public FeeEstimateResponse execute(Client client, Duration timeout) {
        Objects.requireNonNull(client, "client must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");

        var chunkTransactions = buildChunkTransactions(client);
        List<FeeEstimateResponse> responses = new ArrayList<>(chunkTransactions.size());
        for (var chunkTransaction : chunkTransactions) {
            responses.add(executeOnce(client, timeout, chunkTransaction));
        }

        if (responses.isEmpty()) {
            throw new IllegalStateException("Expected at least one fee estimate response");
        }

        FeeEstimateResponse aggregated = responses.get(0);
        for (int i = 1; i < responses.size(); i++) {
            aggregated = aggregated.aggregate(responses.get(i));
        }

        return aggregated;
    }

    /**
     * Execute the query asynchronously using the client's default request timeout.
     *
     * @param client the client
     * @return a future that resolves to the aggregated fee estimate
     */
    public CompletableFuture<FeeEstimateResponse> executeAsync(Client client) {
        return executeAsync(client, client.getRequestTimeout());
    }

    /**
     * Execute the query asynchronously using the provided timeout.
     *
     * @param client  the client
     * @param timeout the timeout
     * @return a future that resolves to the aggregated fee estimate
     */
    public CompletableFuture<FeeEstimateResponse> executeAsync(Client client, Duration timeout) {
        return CompletableFuture.supplyAsync(() -> execute(client, timeout), client.executor);
    }

    private List<com.hedera.hashgraph.sdk.proto.Transaction> buildChunkTransactions(Client client) {
        if (transaction == null) {
            throw new IllegalStateException("transaction must be set");
        }

        if (!transaction.isFrozen()) {
            transaction.freezeWith(client);
        }

        transaction.buildAllTransactions();

        int nodeCount = transaction.nodeAccountIds.size();
        if (nodeCount == 0) {
            throw new IllegalStateException("transaction must specify at least one node account ID");
        }

        int chunkCount = transaction.transactionIds.size();
        if (chunkCount == 0) {
            throw new IllegalStateException("transaction must contain at least one transaction ID");
        }

        List<com.hedera.hashgraph.sdk.proto.Transaction> chunks = new ArrayList<>(chunkCount);
        for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
            int offset = chunkIndex * nodeCount;
            chunks.add(transaction.outerTransactions.get(offset));
        }
        return chunks;
    }

    private FeeEstimateResponse executeOnce(
        Client client, Duration timeout, com.hedera.hashgraph.sdk.proto.Transaction chunkTransaction) {
        var deadline = Deadline.after(timeout.toMillis(), TimeUnit.MILLISECONDS);

        for (int attempt = 1; true; attempt++) {
            try {
                var requestBuilder = com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateQuery.newBuilder()
                    .setTransaction(chunkTransaction);
                if (mode != null) {
                    requestBuilder.setMode(mode.toProtobuf());
                }

                var response = ClientCalls.blockingUnaryCall(
                    buildCall(client, deadline), requestBuilder.build());
                return FeeEstimateResponse.fromProtobuf(response);
            } catch (StatusRuntimeException ex) {
                if (attempt >= maxAttempts || !shouldRetry(ex)) {
                    throw ex;
                }

                warnAndDelay(attempt, ex);
            }
        }
    }

    private ClientCall<com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateQuery, com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateResponse>
    buildCall(Client client, Deadline deadline) {
        try {
            return client.mirrorNetwork
                .getNextMirrorNode()
                .getChannel()
                .newCall(NetworkServiceGrpc.getGetFeeEstimateMethod(), CallOptions.DEFAULT.withDeadline(deadline));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while getting next mirror node", e);
        }
    }

    private static boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof StatusRuntimeException statusRuntimeException) {
            var code = statusRuntimeException.getStatus().getCode();
            return code == io.grpc.Status.Code.UNAVAILABLE || code == io.grpc.Status.Code.DEADLINE_EXCEEDED;
        }
        return false;
    }

    private void warnAndDelay(int attempt, Throwable error) {
        long minBackoffMillis = Client.DEFAULT_MIN_BACKOFF.toMillis();
        long exponentialBackoff = minBackoffMillis << Math.min(Math.max(0, attempt - 1), 30);
        long delay = Math.min(exponentialBackoff, maxBackoff.toMillis());

        LOGGER.warn(
            "Retrying fee estimate query after attempt #{} failed with {}. Waiting {} ms before retrying.",
            attempt,
            error.getMessage(),
            delay);

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
