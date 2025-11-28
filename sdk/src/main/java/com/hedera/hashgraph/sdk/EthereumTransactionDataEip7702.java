// SPDX-License-Identifier: Apache-2.0
    package com.hedera.hashgraph.sdk;

import com.esaulpaugh.headlong.rlp.RLPDecoder;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.esaulpaugh.headlong.util.Integers;
import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.hedera.hashgraph.sdk.EthereumTransactionData;
import org.bouncycastle.util.encoders.Hex;

/**
 * The ethereum transaction data, in the format defined in
 * <a href="https://eips.ethereum.org/EIPS/eip-7702">EIP-7702</a>.
 */
public class EthereumTransactionDataEip7702 extends EthereumTransactionData {

    /**
     * ID of the chain
     */
    public byte[] chainId;

    /**
     * Transaction's nonce
     */
    public byte[] nonce;

    /**
     * Max priority fee per gas
     */
    public byte[] maxPriorityFeePerGas;

    /**
     * Max fee per gas
     */
    public byte[] maxFeePerGas;

    /**
     * The amount of gas available for the transaction
     */
    public byte[] gasLimit;

    /**
     * The receiver of the transaction
     */
    public byte[] to;

    /**
     * The transaction value
     */
    public byte[] value;

    /**
     * Access list entries
     */
    public List<AccessListEntry> accessList;

    /**
     * Authorization tuples as defined in EIP-7702
     */
    public List<AuthorizationTuple> authorizationList;

    /**
     * recovery parameter used to ease the signature verification
     */
    public byte[] signatureYParity;

    /**
     * The R value of the signature
     */
    public byte[] r;

    /**
     * The S value of the signature
     */
    public byte[] s;

    EthereumTransactionDataEip7702(
        byte[] chainId,
        byte[] nonce,
        byte[] maxPriorityFeePerGas,
        byte[] maxFeePerGas,
        byte[] gasLimit,
        byte[] to,
        byte[] value,
        byte[] callData,
        List<AccessListEntry> accessList,
        List<AuthorizationTuple> authorizationList,
        byte[] signatureYParity,
        byte[] r,
        byte[] s) {
        super(callData);

        this.chainId = chainId;
        this.nonce = nonce;
        this.maxPriorityFeePerGas = maxPriorityFeePerGas;
        this.maxFeePerGas = maxFeePerGas;
        this.gasLimit = gasLimit;
        this.to = to;
        this.value = value;
        this.accessList = accessList;
        this.authorizationList = authorizationList;
        this.signatureYParity = signatureYParity;
        this.r = r;
        this.s = s;
    }

    /**
     * Convert a byte array to an ethereum transaction data.
     *
     * @param bytes                     the byte array
     * @return                          the ethereum transaction data
     */
    public static EthereumTransactionDataEip7702 fromBytes(byte[] bytes) {
        var decoder = RLPDecoder.RLP_STRICT.sequenceIterator(bytes);
        var rlpItem = decoder.next();

        byte typeByte = rlpItem.asByte();
        if (typeByte != 0x04) {
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

        var accessList = parseAccessList(rlpList.get(8));
        var authorizationList = parseAuthorizationList(rlpList.get(9));

        if (authorizationList.isEmpty()) {
            throw new IllegalArgumentException("authorization list must not be empty");
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
            accessList,
            authorizationList,
            rlpList.get(10).data(),
            rlpList.get(11).data(),
            rlpList.get(12).data());
    }

    public byte[] toBytes() {
        if (authorizationList == null || authorizationList.isEmpty()) {
            throw new IllegalStateException("authorization list must not be empty");
        }

        return RLPEncoder.sequence(
            Integers.toBytes(0x04),
            List.of(
                chainId,
                nonce,
                maxPriorityFeePerGas,
                maxFeePerGas,
                gasLimit,
                to,
                value,
                callData,
                encodeAccessList(accessList),
                encodeAuthorizationList(authorizationList),
                signatureYParity,
                r,
                s));
    }

    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("chainId", Hex.toHexString(chainId))
            .add("nonce", Hex.toHexString(nonce))
            .add("maxPriorityFeePerGas", Hex.toHexString(maxPriorityFeePerGas))
            .add("maxFeePerGas", Hex.toHexString(maxFeePerGas))
            .add("gasLimit", Hex.toHexString(gasLimit))
            .add("to", Hex.toHexString(to))
            .add("value", Hex.toHexString(value))
            .add("callData", Hex.toHexString(callData))
            .add("accessList", accessList)
            .add("authorizationList", authorizationList)
            .add("signatureYParity", Hex.toHexString(signatureYParity))
            .add("r", Hex.toHexString(r))
            .add("s", Hex.toHexString(s))
            .toString();
    }

    private static List<AccessListEntry> parseAccessList(RLPItem item) {
        List<AccessListEntry> entries = new ArrayList<>();
        for (var entry : item.asRLPList().elements()) {
            var entryList = entry.asRLPList().elements();
            if (entryList.size() != 2) {
                throw new IllegalArgumentException("expected access list entry to have 2 elements");
            }
            var storageKeys = new ArrayList<byte[]>();
            for (var storageKey : entryList.get(1).asRLPList().elements()) {
                storageKeys.add(storageKey.data());
            }
            entries.add(new AccessListEntry(entryList.get(0).data(), storageKeys));
        }
        return entries;
    }

    private static List<AuthorizationTuple> parseAuthorizationList(RLPItem item) {
        List<AuthorizationTuple> tuples = new ArrayList<>();
        for (var tuple : item.asRLPList().elements()) {
            var tupleElements = tuple.asRLPList().elements();
            if (tupleElements.size() != 6) {
                throw new IllegalArgumentException("expected authorization tuple to have 6 elements");
            }
            tuples.add(new AuthorizationTuple(
                tupleElements.get(0).data(),
                tupleElements.get(1).data(),
                tupleElements.get(2).data(),
                tupleElements.get(3).data(),
                tupleElements.get(4).data(),
                tupleElements.get(5).data()));
        }
        return tuples;
    }

    private static List<Object> encodeAccessList(List<AccessListEntry> accessList) {
        List<Object> entries = new ArrayList<>();
        for (var entry : accessList) {
            List<Object> encodedEntry = new ArrayList<>();
            encodedEntry.add(entry.address);
            List<Object> storageKeys = new ArrayList<>();
            for (var storageKey : entry.storageKeys) {
                storageKeys.add(storageKey);
            }
            encodedEntry.add(storageKeys);
            entries.add(encodedEntry);
        }
        return entries;
    }

    private static List<Object> encodeAuthorizationList(List<AuthorizationTuple> authorizationList) {
        List<Object> tuples = new ArrayList<>();
        for (var tuple : authorizationList) {
            List<Object> encodedTuple = new ArrayList<>();
            encodedTuple.add(tuple.chainId);
            encodedTuple.add(tuple.address);
            encodedTuple.add(tuple.nonce);
            encodedTuple.add(tuple.yParity);
            encodedTuple.add(tuple.r);
            encodedTuple.add(tuple.s);
            tuples.add(encodedTuple);
        }
        return tuples;
    }

    /**
     * Access list entry definition
     */
    public static class AccessListEntry {
        /**
         * Address covered by the access list entry
         */
        public final byte[] address;

        /**
         * Storage keys covered by the access list entry
         */
        public final List<byte[]> storageKeys;

        public AccessListEntry(byte[] address, List<byte[]> storageKeys) {
            this.address = Objects.requireNonNull(address);
            this.storageKeys = Objects.requireNonNull(storageKeys);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("address", Hex.toHexString(address))
                .add("storageKeys", storageKeys.stream().map(Hex::toHexString).toList())
                .toString();
        }
    }

    /**
     * Authorization tuple as defined in EIP-7702
     */
    public static class AuthorizationTuple {
        /** Chain Id for the authorized signature */
        public final byte[] chainId;
        /** Authorized address */
        public final byte[] address;
        /** Nonce for the authorization */
        public final byte[] nonce;
        /** Signature yParity */
        public final byte[] yParity;
        /** Signature R */
        public final byte[] r;
        /** Signature S */
        public final byte[] s;

        public AuthorizationTuple(byte[] chainId, byte[] address, byte[] nonce, byte[] yParity, byte[] r, byte[] s) {
            this.chainId = Objects.requireNonNull(chainId);
            this.address = Objects.requireNonNull(address);
            this.nonce = Objects.requireNonNull(nonce);
            this.yParity = Objects.requireNonNull(yParity);
            this.r = Objects.requireNonNull(r);
            this.s = Objects.requireNonNull(s);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("chainId", Hex.toHexString(chainId))
                .add("address", Hex.toHexString(address))
                .add("nonce", Hex.toHexString(nonce))
                .add("yParity", Hex.toHexString(yParity))
                .add("r", Hex.toHexString(r))
                .add("s", Hex.toHexString(s))
                .toString();
        }
    }
}
