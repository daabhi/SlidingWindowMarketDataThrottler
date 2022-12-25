package throttler;

import org.junit.jupiter.api.Test;
import pojo.ConflatingQueue;
import pojo.MarketData;
import pojo.Price;

import java.time.Instant;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConflationQueueTest {
    ConflatingQueue conflatingQueue = new ConflatingQueue(10000);

    @Test
    public void test(){
        MarketData marketData = new MarketData();
        marketData.setPrice(new Price(1,1,1));
        marketData.setSymbol("A");
        marketData.setUpdateTime(Instant.now());
        IntStream.range(0, 1000).forEach(i -> conflatingQueue.offer(marketData));
        conflatingQueue.take();
        assertEquals(0, conflatingQueue.getSymbols().size(),"Single symbol take ensures that the data is already conflated");
        assertEquals(0, conflatingQueue.getMarketDataSymbolMap().size(), "Single symbol take ensures that data is already conflated");
    }
}
