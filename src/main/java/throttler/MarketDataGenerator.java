package throttler;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import pojo.MarketData;
import pojo.Price;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter @Setter @ToString
public class MarketDataGenerator {
    private Queue<MarketData> mockMarketData = new ArrayDeque<>(1000);
    private int noOfSymbols;
    private int noOfRecordsPerSymbol;
    private static int count = 1;
    public MarketDataGenerator(int noOfSymbols, int noOfRecordsPerSymbol){
        this.noOfSymbols = noOfSymbols;
        this.noOfRecordsPerSymbol = noOfRecordsPerSymbol;
    }

    public Queue<MarketData> generateMockMarketData() {
        count++;
        Queue<MarketData> mockData = new ArrayDeque<>();
        List<String> mockSymbols = IntStream.range(0, noOfSymbols).mapToObj(i -> "A" + i).collect(Collectors.toCollection(() -> new ArrayList<>(100)));
        for (int i = 1; i <= noOfRecordsPerSymbol; i++) {
            for (String symbol : mockSymbols) {
                MarketData data = new MarketData(Instant.now().plus(count, ChronoUnit.MILLIS),symbol,new Price(count+i, count+i + i, count+i - 1));
                mockData.offer(data);
            }
        }
        mockMarketData.addAll(mockData);
        return mockData;
    }
}
