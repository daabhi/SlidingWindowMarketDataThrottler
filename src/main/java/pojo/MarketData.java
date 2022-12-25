package pojo;

import lombok.*;

import java.time.Instant;


@NoArgsConstructor @Getter @Setter @EqualsAndHashCode @ToString
public class MarketData {
    private Instant updateTime;
    private String  symbol;
    private Price   price;

    public MarketData(Instant updateTime, String symbol, Price price){
        this.updateTime = updateTime;
        this.symbol     = symbol;
        this.price      = price;
    }
}