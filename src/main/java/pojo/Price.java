package pojo;

import lombok.*;


@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class Price {
    private double bid;
    private double ask;
    private double last;

    public Price(double bid, double ask, double last){
        this.bid  = bid;
        this.ask  = ask;
        this.last = last;
    }

    public Price(Price price) {
        this.bid  = price.bid;
        this.ask  = price.ask;
        this.last = price.last;
    }
}