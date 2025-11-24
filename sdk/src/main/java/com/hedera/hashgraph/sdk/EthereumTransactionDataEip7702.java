// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.esaulpaugh.headlong.rlp.RLPDecoder;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.esaulpaugh.headlong.rlp.RLPList;
import com.esaulpaugh.headlong.util.Integers;
import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.stream.Collectors;
import org.bouncycastle.util.encoders.Hex;

/**
 * The ethereum transaction data, in the format defined in
 * <a href="https://eips.ethereum.org/EIPS/eip-7702">EIP-7702</a>.
 */
public class EthereumTransactionDataEip7702 extends EthereumTransactionData {

    /**
     * ID of the chain.
     */
    public byte[] chainId;

    /**
     * Transaction's nonce.
     */
    public byte[] nonce;

    /**
     * Priority fee per gas.
     */
    public byte[] maxPriorityGas;

    /**
     * Maximum fee per gas.
     */
    public byte[] maxGas;

    /**
     * The amount of gas available for the transaction.
     */
    public byte[] gasLimit;

    /**
     * The receiver of the transaction.
     */
    public byte[] to;

    /**
     * The transaction value.
     */
    public byte[] value;

    /**
     * Specifies an array of addresses and storage keys that the transaction plans to access.
     */
    public RLPList accessList;

    /**
     * The authorization tuples associated with the transaction.
     */
    public RLPList authorizationList;

    /**
     * Recovery parameter used to ease the signature verification.
     */
    public byte[] recoveryId;

    /**
     * The R value of the signature.
     */
    public byte[] r;

    /**
     * The S value of the signature.
     */
    public byte[] s;

    EthereumTransactionDataEip7702(
        byte[] chainId,
        byte[] nonce,
        byte[] maxPriorityGas,
        byte[] maxGas,
        byte[] gasLimit,
        byte[] to,
        byte[] value,
        byte[] callData,
        RLPList accessList,
        RLPList authorizationList,
        byte[] recoveryId,
        byte[] r,
        byte[] s) {
        super(callData);

        this.chainId = chainId;
        this.nonce = nonce;
        this.maxPriorityGas = maxPriorityGas;
        this.maxGas = maxGas;
        this.gasLimit = gasLimit;
        this.to = to;
        this.value = value;
        this.accessList = accessList;
        this.authorizationList = authorizationList;
        this.recoveryId = recoveryId;
        this.r = r;
        this.s = s;
    }

    /**
     * Convert a byte array to an ethereum transaction data.
     *
     * @param bytes the byte array
     * @return the ethereum transaction data
     */
    public static EthereumTransactionDataEip7702 fromBytes(byte[] bytes) {
        var decoder = RLPDecoder.RLP_STRICT.sequenceIterator(bytes);
        var rlpItem = decoder.next();

        // typed transaction?
        byte typeByte = rlpItem.asByte();
        if (typeByte != 4) {
            throw new IllegalArgumentException("rlp type byte " + typeByte + "is not supported");
        }

        rlpItem = decoder.next();
        if (!rlpItem.isList()) {
            throw new IllegalArgumentException("expected RLP element list");
        }

        List<RLPItem> rlpList = rlpItem.asRLPList().elements();
        if (rlpList.size() != 13) {
            throw new IllegalArgumentException("expected 13 RLP encoded elements, found " + rlpList.size());
        }

        return new EthereumTransactionDataEip7702(
            rlpList.get(0).data(),
            rlpList.get(1).data(),
            rlpList.get(2).data(),
            rlpList.get(3).data(),
            rlpList.get(4).data(),
            rlpList.get(5).data(),
            rlpList.get(6).data(),
            rlpList.get(7).data(),
            rlpList.get(8).asRLPList(),
            rlpList.get(9).asRLPList(),
            rlpList.get(10).data(),
            rlpList.get(11).data(),
            rlpList.get(12).data());
    }

    public byte[] toBytes() {
        return RLPEncoder.sequence(
            Integers.toBytes(0x04),
            List.of(
                chainId,
                nonce,
                maxPriorityGas,
                maxGas,
                gasLimit,
                to,
                value,
                callData,
                convertRlpList(accessList),
                convertRlpList(authorizationList),
                recoveryId,
                r,
                s));
    }

    private static List<Object> convertRlpList(RLPList list) {
        return list.elements().stream()
            .map(EthereumTransactionDataEip7702::convertRlpItem)
            .collect(Collectors.toList());
    }

    private static Object convertRlpItem(RLPItem item) {
        if (item.isList()) {
            return convertRlpList(item.asRLPList());
        }

        return item.data();
    }

    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("chainId", Hex.toHexString(chainId))
            .add("nonce", Hex.toHexString(nonce))
            .add("maxPriorityGas", Hex.toHexString(maxPriorityGas))
            .add("maxGas", Hex.toHexString(maxGas))
            .add("gasLimit", Hex.toHexString(gasLimit))
            .add("to", Hex.toHexString(to))
            .add("value", Hex.toHexString(value))
            .add("accessList", Hex.toHexString(accessList.encoding()))
            .add("authorizationList", Hex.toHexString(authorizationList.encoding()))
            .add("recoveryId", Hex.toHexString(recoveryId))
            .add("r", Hex.toHexString(r))
            .add("s", Hex.toHexString(s))
            .toString();
    }
}
