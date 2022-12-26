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
    public void testMultipleSameKeyAddAndRetrieve(){
        IntStream.range(0, 1000).forEach(i -> conflatingQueue.offer(MarketData.builder().price(new Price(1,1,1)).symbol("A").updateTime(Instant.now()).build()));
        conflatingQueue.take();
        assertEquals(0, conflatingQueue.getSymbols().size(),"Single symbol take ensures that the data is already conflated");
        assertEquals(0, conflatingQueue.getMarketDataSymbolMap().size(), "Single symbol take ensures that data is already conflated");
    }

    @Test
    public void testMultipleDiffKeyAddAndRetrieve(){
        IntStream.range(0, 1000).forEach(i -> conflatingQueue.offer(MarketData.builder().price(new Price(1,1,1)).symbol("A").updateTime(Instant.now()).build()));
        IntStream.range(0, 1000).forEach(i -> conflatingQueue.offer(MarketData.builder().price(new Price(1,1,1)).symbol("B").updateTime(Instant.now()).build()));
        IntStream.range(0, 1000).forEach(i -> conflatingQueue.offer(MarketData.builder().price(new Price(3,3,3)).symbol("A").updateTime(Instant.now()).build()));
        MarketData take = conflatingQueue.take();
        assertEquals("Price(bid=3.0, ask=3.0, last=3.0)",take.getPrice().toString());
        assertEquals("A",take.getSymbol(),"As A got published latest after B , so the take gets the latest market data (value 3) for the latest symbol(A)");

        MarketData secondTake = conflatingQueue.take();
        assertEquals("Price(bid=1.0, ask=1.0, last=1.0)",secondTake.getPrice().toString());
        assertEquals("B",secondTake.getSymbol(),"As B got published so the take gets the latest market data (value 1) for the latest symbol(B)");

        IntStream.range(0, 1000).forEach(i -> conflatingQueue.offer(MarketData.builder().price(new Price(4,4,4)).symbol("B").updateTime(Instant.now()).build()));
        IntStream.range(0, 1000).forEach(i -> conflatingQueue.offer(MarketData.builder().price(new Price(5,5,5)).symbol("B").updateTime(Instant.now()).build()));
        IntStream.range(0, 1000).forEach(i -> conflatingQueue.offer(MarketData.builder().price(new Price(6,6,6)).symbol("B").updateTime(Instant.now()).build()));
        MarketData thirdTake = conflatingQueue.take();
        assertEquals("Price(bid=6.0, ask=6.0, last=6.0)",thirdTake.getPrice().toString());
        assertEquals("B",thirdTake.getSymbol(),"As B got published so the take gets the latest market data (value 6) for the latest symbol(B)");

    }


}
