package org.coin.price.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
public class PriceApiRequest {
    @JsonProperty("status")
    String status;
    @JsonProperty("data")
    InnerData data;

    @Getter
    @NoArgsConstructor
    public static class InnerData {
        @JsonProperty("date")
        private Long timestamp;
        @JsonAnySetter
        private Map<String, PriceData> priceDataMap;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties({
            "opening_price",
            "min_price",
            "max_price",
            "units_traded",
            "acc_trade_value",
            "prev_closing_price",
            "units_traded_24H",
            "acc_trade_value_24H",
            "fluctate_24H",
            "fluctate_rate_24H"})
    public static class PriceData {
        private String closing_price;
    }
}

