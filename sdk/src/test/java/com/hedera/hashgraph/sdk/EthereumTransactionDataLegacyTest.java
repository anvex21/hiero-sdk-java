// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.util.Integers;
import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

public class EthereumTransactionDataLegacyTest {
    // https://github.com/hashgraph/hedera-services/blob/1e01d9c6b8923639b41359c55413640b589c4ec7/hapi-utils/src/test/java/com/hedera/services/ethereum/EthTxDataTest.java#L49
    static final String RAW_TX_TYPE_0 =
        "f864012f83018000947e3a9eaf9bcc39e2ffa38eb30bf7a93feacbc18180827653820277a0f9fbff985d374be4a55f296915002eec11ac96f1ce2df183adf992baa9390b2fa00c1e867cc960d9c74ec2e6a662b7908ec4c8cc9f3091e886bcefbeb2290fb792";
    static final String RAW_TX_TYPE_0_TRIMMED_LAST_BYTES =
        "f864012f83018000947e3a9eaf9bcc39e2ffa38eb30bf7a93feacbc18180827653820277a0f9fbff985d374be4a55f296915002eec11ac96f1ce2df183adf992baa9390b2fa00c1e867cc960d9c74ec2e6a662b7908ec4c8cc9f3091e886bcefbeb2290000";
    static final String RAW_TX_TYPE_2 =
        "02f87082012a022f2f83018000947e3a9eaf9bcc39e2ffa38eb30bf7a93feacbc181880de0b6b3a764000083123456c001a0df48f2efd10421811de2bfb125ab75b2d3c44139c4642837fb1fccce911fd479a01aaf7ae92bee896651dfc9d99ae422a296bf5d9f1ca49b2d96d82b79eb112d66";
    static final byte[] CHAIN_ID = Hex.decode("012a");
    static final byte[] NONCE = Hex.decode("03");
    static final byte[] MAX_PRIORITY = Hex.decode("2f");
    static final byte[] MAX_FEE = Hex.decode("30");
    static final byte[] GAS_LIMIT = Hex.decode("018000");
    static final byte[] TO = Hex.decode("7e3a9eaf9bcc39e2ffa38eb30bf7a93feacbc181");
    static final byte[] VALUE = Hex.decode("0de0b6b3a7640000");
    static final byte[] CALL_DATA = Hex.decode("12345678");
    static final byte[] ACCESS_ADDRESS = Hex.decode("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    static final byte[] ACCESS_STORAGE_KEY =
        Hex.decode("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
    static final byte[] AUTH_CHAIN_ID = Hex.decode("012a");
    static final byte[] AUTH_ADDRESS = Hex.decode("cccccccccccccccccccccccccccccccccccccccc");
    static final byte[] AUTH_NONCE = Hex.decode("01");
    static final byte[] AUTH_Y_PARITY = Hex.decode("01");
    static final byte[] AUTH_R =
        Hex.decode("1111111111111111111111111111111111111111111111111111111111111111");
    static final byte[] AUTH_S =
        Hex.decode("2222222222222222222222222222222222222222222222222222222222222222");
    static final byte[] SIG_Y_PARITY = Hex.decode("01");
    static final byte[] SIG_R =
        Hex.decode("3333333333333333333333333333333333333333333333333333333333333333");
    static final byte[] SIG_S =
        Hex.decode("4444444444444444444444444444444444444444444444444444444444444444");

    static final List<Object> ACCESS_LIST_ENCODING =
        List.of(List.of(ACCESS_ADDRESS, List.of(ACCESS_STORAGE_KEY)));
    static final List<Object> AUTH_LIST_ENCODING =
        List.of(List.of(AUTH_CHAIN_ID, AUTH_ADDRESS, AUTH_NONCE, AUTH_Y_PARITY, AUTH_R, AUTH_S));

    static final String RAW_TX_TYPE_4 = Hex.toHexString(RLPEncoder.sequence(
        Integers.toBytes(0x04),
        List.of(
            CHAIN_ID,
            NONCE,
            MAX_PRIORITY,
            MAX_FEE,
            GAS_LIMIT,
            TO,
            VALUE,
            CALL_DATA,
            ACCESS_LIST_ENCODING,
            AUTH_LIST_ENCODING,
            SIG_Y_PARITY,
            SIG_R,
            SIG_S)));

    @Test
    public void legacyToFromBytes() {
        var data = (EthereumTransactionDataLegacy) EthereumTransactionData.fromBytes(Hex.decode(RAW_TX_TYPE_0));
        assertThat(RAW_TX_TYPE_0).isEqualTo(Hex.toHexString(data.toBytes()));

        // Chain ID is not part of the legacy ethereum transaction, so why are you calculating and checking it?
        // assertEquals("012a", Hex.toHexString(data.chainId()));

        assertThat(Hex.toHexString(data.nonce)).isEqualTo("01");
        assertThat(Hex.toHexString(data.gasPrice)).isEqualTo("2f");
        assertThat(Hex.toHexString(data.gasLimit)).isEqualTo("018000");
        assertThat(Hex.toHexString(data.to)).isEqualTo("7e3a9eaf9bcc39e2ffa38eb30bf7a93feacbc181");
        assertThat(Hex.toHexString(data.value)).isEqualTo("");
        assertThat(Hex.toHexString(data.callData)).isEqualTo("7653");
        assertThat(Hex.toHexString(data.v)).isEqualTo("0277");
        assertThat(Hex.toHexString(data.r))
            .isEqualTo("f9fbff985d374be4a55f296915002eec11ac96f1ce2df183adf992baa9390b2f");
        assertThat(Hex.toHexString(data.s))
            .isEqualTo("0c1e867cc960d9c74ec2e6a662b7908ec4c8cc9f3091e886bcefbeb2290fb792");

        // We don't currently support a way to get the ethereum has, but we probably should
        // assertEquals("9ffbd69c44cf643ed8d1e756b505e545e3b5dd3a6b5ef9da1d8eca6679706594",
        //    Hex.toHexString(data.getEthereumHash()));
    }

    @Test
    public void eip1559ToFromBytes() {
        var data = (EthereumTransactionDataEip1559) EthereumTransactionData.fromBytes(Hex.decode(RAW_TX_TYPE_2));
        assertThat(RAW_TX_TYPE_2).isEqualTo(Hex.toHexString(data.toBytes()));

        assertThat(Hex.toHexString(data.chainId)).isEqualTo("012a");
        assertThat(Hex.toHexString(data.nonce)).isEqualTo("02");
        assertThat(Hex.toHexString(data.maxPriorityGas)).isEqualTo("2f");
        assertThat(Hex.toHexString(data.maxGas)).isEqualTo("2f");
        assertThat(Hex.toHexString(data.gasLimit)).isEqualTo("018000");
        assertThat(Hex.toHexString(data.to)).isEqualTo("7e3a9eaf9bcc39e2ffa38eb30bf7a93feacbc181");
        assertThat(Hex.toHexString(data.value)).isEqualTo("0de0b6b3a7640000");
        assertThat(Hex.toHexString(data.callData)).isEqualTo("123456");
        assertThat(Hex.toHexString(data.accessList)).isEqualTo("");
        assertThat(Hex.toHexString(data.recoveryId)).isEqualTo("01");
        assertThat(Hex.toHexString(data.r))
            .isEqualTo("df48f2efd10421811de2bfb125ab75b2d3c44139c4642837fb1fccce911fd479");
        assertThat(Hex.toHexString(data.s))
            .isEqualTo("1aaf7ae92bee896651dfc9d99ae422a296bf5d9f1ca49b2d96d82b79eb112d66");
    }

    @Test
    void eip7702ToFromBytes() {
        var data = (EthereumTransactionDataEip7702) EthereumTransactionData.fromBytes(Hex.decode(RAW_TX_TYPE_4));

        assertThat(RAW_TX_TYPE_4).isEqualTo(Hex.toHexString(data.toBytes()));

        assertThat(Hex.toHexString(data.chainId)).isEqualTo("012a");
        assertThat(Hex.toHexString(data.nonce)).isEqualTo("03");
        assertThat(Hex.toHexString(data.maxPriorityFeePerGas)).isEqualTo("2f");
        assertThat(Hex.toHexString(data.maxFeePerGas)).isEqualTo("30");
        assertThat(Hex.toHexString(data.gasLimit)).isEqualTo("018000");
        assertThat(Hex.toHexString(data.to)).isEqualTo("7e3a9eaf9bcc39e2ffa38eb30bf7a93feacbc181");
        assertThat(Hex.toHexString(data.value)).isEqualTo("0de0b6b3a7640000");
        assertThat(Hex.toHexString(data.callData)).isEqualTo("12345678");
        assertThat(data.accessList).hasSize(1);
        assertThat(Hex.toHexString(data.accessList.get(0).address))
            .isEqualTo("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        assertThat(Hex.toHexString(data.accessList.get(0).storageKeys.get(0)))
            .isEqualTo("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        assertThat(data.authorizationList).hasSize(1);
        assertThat(Hex.toHexString(data.authorizationList.get(0).chainId)).isEqualTo("012a");
        assertThat(Hex.toHexString(data.authorizationList.get(0).address))
            .isEqualTo("cccccccccccccccccccccccccccccccccccccccc");
        assertThat(Hex.toHexString(data.authorizationList.get(0).nonce)).isEqualTo("01");
        assertThat(Hex.toHexString(data.authorizationList.get(0).yParity)).isEqualTo("01");
        assertThat(Hex.toHexString(data.authorizationList.get(0).r))
            .isEqualTo("1111111111111111111111111111111111111111111111111111111111111111");
        assertThat(Hex.toHexString(data.authorizationList.get(0).s))
            .isEqualTo("2222222222222222222222222222222222222222222222222222222222222222");
        assertThat(Hex.toHexString(data.signatureYParity)).isEqualTo("01");
        assertThat(Hex.toHexString(data.r))
            .isEqualTo("3333333333333333333333333333333333333333333333333333333333333333");
        assertThat(Hex.toHexString(data.s))
            .isEqualTo("4444444444444444444444444444444444444444444444444444444444444444");
    }

    @Test
    void throwsOnEmptyAuthorizationList() {
        var accessList = new ArrayList<EthereumTransactionDataEip7702.AccessListEntry>();
        accessList.add(new EthereumTransactionDataEip7702.AccessListEntry(
            ACCESS_ADDRESS, List.of(ACCESS_STORAGE_KEY)));

        var data = new EthereumTransactionDataEip7702(
            CHAIN_ID,
            NONCE,
            MAX_PRIORITY,
            MAX_FEE,
            GAS_LIMIT,
            TO,
            VALUE,
            CALL_DATA,
            accessList,
            List.of(),
            SIG_Y_PARITY,
            SIG_R,
            SIG_S);

        assertThrows(IllegalStateException.class, data::toBytes);
    }
}
