package org.coin.price.dto;

import java.util.Comparator;

public class CryptoCoinComparator implements Comparator<CryptoCoin> {
    @Override
    public int compare(CryptoCoin o1, CryptoCoin o2) {
        return Long.compare(o1.getTimestamp(), o2.getTimestamp());
    }
}
