package com.hedera.hashgraph.sdk;

import java.util.Objects;

/**
 * The extra fee charged for the transaction.
 */
public final class FeeExtra {
    /** The unique name of this extra fee. */
    public final String name;
    /** The count that is included for free. */
    public final long included;
    /** The actual count of items. */
    public final long count;
    /** The charged count after subtracting the included value. */
    public final long charged;
    /** The fee price per unit in tinycents. */
    public final long feePerUnit;
    /** The subtotal of this extra in tinycents. */
    public final long subtotal;

    private FeeExtra(String name, long included, long count, long charged, long feePerUnit, long subtotal) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.included = included;
        this.count = count;
        this.charged = charged;
        this.feePerUnit = feePerUnit;
        this.subtotal = subtotal;
    }

    static FeeExtra fromProtobuf(com.hedera.hashgraph.sdk.proto.mirror.FeeExtra proto) {
        return new FeeExtra(
            proto.getName(),
            Integer.toUnsignedLong(proto.getIncluded()),
            Integer.toUnsignedLong(proto.getCount()),
            Integer.toUnsignedLong(proto.getCharged()),
            proto.getFeePerUnit(),
            proto.getSubtotal());
    }

    com.hedera.hashgraph.sdk.proto.mirror.FeeExtra toProtobuf() {
        return com.hedera.hashgraph.sdk.proto.mirror.FeeExtra.newBuilder()
            .setName(name)
            .setIncluded((int) included)
            .setCount((int) count)
            .setCharged((int) charged)
            .setFeePerUnit(feePerUnit)
            .setSubtotal(subtotal)
            .build();
    }

    FeeExtra aggregate(FeeExtra other) {
        if (!name.equals(other.name)) {
            throw new IllegalArgumentException("Cannot aggregate FeeExtra with different names");
        }
        if (feePerUnit != other.feePerUnit) {
            throw new IllegalArgumentException("Cannot aggregate FeeExtra with different feePerUnit values");
        }

        long totalCount = Math.addExact(count, other.count);
        long totalCharged = Math.addExact(charged, other.charged);
        long totalSubtotal = Math.addExact(subtotal, other.subtotal);
        long inferredIncluded = Math.max(0L, totalCount - totalCharged);

        return new FeeExtra(name, inferredIncluded, totalCount, totalCharged, feePerUnit, totalSubtotal);
    }

    @Override
    public String toString() {
        return "FeeExtra{"
            + "name='" + name + '\''
            + ", included=" + included
            + ", count=" + count
            + ", charged=" + charged
            + ", feePerUnit=" + feePerUnit
            + ", subtotal=" + subtotal
            + '}';
    }
}
