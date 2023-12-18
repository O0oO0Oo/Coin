package org.coin.order.dto.projection;

public interface FindOrderView {
    Long getId();
    String getCryptoName();
    Double getQuantity();
    boolean getProcessed();
    boolean getCanceled();
}