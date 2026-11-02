package com.acme.salary.domain;

import java.math.BigDecimal;

public enum JobLevel {
    L1(1, "48000"),
    L2(2, "68000"),
    L3(3, "90000"),
    L4(4, "115000"),
    L5(5, "145000"),
    L6(6, "175000"),
    M1(7, "190000"),
    M2(8, "230000"),
    M3(9, "280000");

    private final int rank;
    private final BigDecimal usdMid;

    JobLevel(int rank, String usdMid) {
        this.rank = rank;
        this.usdMid = new BigDecimal(usdMid);
    }

    public int rank() {
        return rank;
    }

    public BigDecimal usdMid() {
        return usdMid;
    }

    public static JobLevel fromRank(int rank) {
        for (JobLevel level : values()) {
            if (level.rank == rank) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown level rank " + rank);
    }
}
