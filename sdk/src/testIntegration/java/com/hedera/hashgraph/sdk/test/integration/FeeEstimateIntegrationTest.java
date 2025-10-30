package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.FeeEstimateMode;
import com.hedera.hashgraph.sdk.FeeEstimateQuery;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TransferTransaction;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeeEstimateIntegrationTest {
    @Test
    @DisplayName("Fee estimate stays within tolerance of the actual charged fee")
    void feeEstimateCloseToActualFee() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var recipientKey = PrivateKey.generateED25519();
            AccountId recipientId = new AccountCreateTransaction()
                .setKeyWithoutAlias(recipientKey)
                .setInitialBalance(Hbar.fromTinybars(1))
                .execute(testEnv.client)
                .getReceipt(testEnv.client)
                .accountId;

            var transferTransaction = new TransferTransaction()
                .addHbarTransfer(testEnv.operatorId, Hbar.fromTinybars(-1_000_000))
                .addHbarTransfer(recipientId, Hbar.fromTinybars(1_000_000));

            var estimate = new FeeEstimateQuery()
                .setMode(FeeEstimateMode.STATE)
                .setTransaction(transferTransaction)
                .execute(testEnv.client);

            var transferRecord = transferTransaction.execute(testEnv.client).getRecord(testEnv.client);
            var exchangeRate = transferRecord.receipt.exchangeRate;
            assertThat(exchangeRate.hbars).isPositive();
            long actualTinycents = Math.floorDiv(
                transferRecord.transactionFee.toTinybars() * (long) exchangeRate.cents,
                (long) exchangeRate.hbars);

            assertThat(estimate.total).isPositive();
            assertThat(actualTinycents).isPositive();

            BigDecimal estimateTinycents = BigDecimal.valueOf(estimate.total);
            BigDecimal actualTinycentsDecimal = BigDecimal.valueOf(actualTinycents);
            BigDecimal diff = estimateTinycents.subtract(actualTinycentsDecimal).abs();
            BigDecimal higher = estimateTinycents.max(actualTinycentsDecimal);

            // Allow a tolerance of 25% difference between estimate and actual fees.
            BigDecimal tolerance = higher.multiply(new BigDecimal("0.25"));

            assertThat(diff.compareTo(tolerance)).isLessThanOrEqualTo(0);

            // Sanity check: estimated components sum to total
            long nodeSubtotal = estimate.node.subtotal();
            long serviceSubtotal = estimate.service.subtotal();
            long expectedNetworkSubtotal = Math.multiplyExact(estimate.network.multiplier, nodeSubtotal);
            long recomputedTotal = Math.addExact(
                Math.addExact(expectedNetworkSubtotal, nodeSubtotal), serviceSubtotal);
            assertThat(estimate.total).isEqualTo(recomputedTotal);
        }
    }
}
