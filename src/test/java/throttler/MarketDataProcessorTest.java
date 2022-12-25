package throttler;

import org.junit.jupiter.api.Test;
import pojo.ConflatingQueue;
import pojo.MarketData;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MarketDataProcessorTest {

    /**
     * 1. Producer publishes 10 records each for 9 symbols at one instant which end up getting conflated in our queue(latest overrides it)
     * 2. throttler.SlidingWindow allows only 3 requests per second
     * 3. We assert that only 3 messages are sent in 1st sec, followed by 3 in each subsequent second.
     */
    @Test
    public void testPublish() throws InterruptedException {
        SlidingWindow slidingWindow = new SlidingWindow(3, 1); //i.e. 3 requests per second
        ConflatingQueue conflatingQueue = new ConflatingQueue(1000000);
        MarketDataGenerator marketDataGenerator = new MarketDataGenerator(9, 10);
        MarketDataProcessor marketDataProcessor = new MarketDataProcessor(marketDataGenerator, conflatingQueue);

        marketDataGenerator.generateMockMarketData().forEach(marketDataProcessor::onMessage);
        assertEquals(9, marketDataProcessor.getConflatingQueue().getNoOfSymbols(), "9 market data events created");

        ThrottledPublisher throttledPublisher = new ThrottledPublisher(conflatingQueue, marketDataProcessor, slidingWindow);
        throttledPublisher.publishData();

        assertEquals("{A0=1, A1=1, A2=1}", throttledPublisher.getPublishCounts().toString());
        //assertEquals("[A3, A4, A5, A6, A7, A8]", throttledPublisher.getPendingQueue().stream().map(pojo.MarketData::getSymbol).collect(Collectors.toList()).toString(), "6 messages throttled as expected, only 3 allowed per second");

        Thread.sleep(1000);
        throttledPublisher.publishData();
        assertEquals("{A0=1, A1=1, A2=1, A3=1, A4=1, A5=1}", throttledPublisher.getPublishCounts().toString(), "6 messages have been published, after 2 secs");

        Thread.sleep(1000);
        throttledPublisher.publishData();
        assertEquals("{A0=1, A1=1, A2=1, A3=1, A4=1, A5=1, A6=1, A7=1, A8=1}", throttledPublisher.getPublishCounts().toString(), "9 messages have been published after 3 secs");
    }

    /**
     * Verify that the conflation logic works as expected to ensure our queue always has the latest market data.
     */
    @Test
    public void testMarketDataGenerationFollowedByConflation() {
        SlidingWindow slidingWindow = new SlidingWindow(3, 1); //i.e. 3 requests per second
        ConflatingQueue conflatingQueue = new ConflatingQueue(1000000);
        MarketDataGenerator marketDataGenerator = new MarketDataGenerator(9, 10);
        MarketDataProcessor marketDataProcessor = new MarketDataProcessor(marketDataGenerator, conflatingQueue);


        marketDataGenerator = new MarketDataGenerator(2, 2);
        List<MarketData> marketData = new ArrayList<>(marketDataGenerator.generateMockMarketData());
        assertEquals(4, marketData.size());

        assertEquals("A0", marketData.get(0).getSymbol());
        assertEquals("A1", marketData.get(1).getSymbol());
        assertEquals("A0", marketData.get(2).getSymbol());
        assertEquals("A1", marketData.get(3).getSymbol());

        assertEquals("Price(bid=4.0, ask=5.0, last=3.0)", marketData.get(0).getPrice().toString());
        assertEquals("Price(bid=4.0, ask=5.0, last=3.0)", marketData.get(1).getPrice().toString());
        assertEquals("Price(bid=5.0, ask=7.0, last=4.0)", marketData.get(2).getPrice().toString());
        assertEquals("Price(bid=5.0, ask=7.0, last=4.0)", marketData.get(3).getPrice().toString());

        marketData.forEach(marketDataProcessor::onMessage);
        assertEquals(2, marketDataProcessor.getConflatingQueue().getNoOfSymbols());
        Queue<String> symbols = marketDataProcessor.getConflatingQueue().getSymbols();

        assertEquals("A0", marketDataProcessor.getConflatingQueue().getMarketData("A0").getSymbol(),"A0 has latest price of 5,7,4 instead of 4,5,3 as expected");
        assertEquals("Price(bid=5.0, ask=7.0, last=4.0)", marketDataProcessor.getConflatingQueue().getMarketData("A1").getPrice().toString(),"A0 has latest price of 5,7,4 instead of 4,5,3 as expected");

        assertEquals("A1", marketDataProcessor.getConflatingQueue().getMarketData("A1").getSymbol());
        assertEquals("Price(bid=5.0, ask=7.0, last=4.0)", marketDataProcessor.getConflatingQueue().getMarketData("A1").getPrice().toString());
    }

    @Test
    public void testEachSymbolGetsPublishedOnlyOncePerSlidingWindow() {
        SlidingWindow slidingWindow = new SlidingWindow(3, 1); //i.e. 3 requests per second
        ConflatingQueue conflatingQueue = new ConflatingQueue(1000000);
        MarketDataGenerator marketDataGenerator = new MarketDataGenerator(9, 10);
        MarketDataProcessor marketDataProcessor = new MarketDataProcessor(marketDataGenerator, conflatingQueue);

        marketDataGenerator = new MarketDataGenerator(4, 10);
        marketDataGenerator.generateMockMarketData().forEach(marketDataProcessor::onMessage);
        assertEquals(4, marketDataProcessor.getConflatingQueue().getNoOfSymbols(), "40 market data events created");

        ThrottledPublisher throttledPublisher = new ThrottledPublisher(conflatingQueue, marketDataProcessor, slidingWindow);
        throttledPublisher.publishData();

        assertEquals("{A0=1, A1=1, A2=1}", throttledPublisher.getPublishCounts().toString(),"Verified that each symbol getting published only once");
    }

    @Test
    public void testNoOfRecordsPublishedDoesNotExceed100PerSlidingWindow(){
        SlidingWindow slidingWindow; //i.e. 3 requests per second
        ConflatingQueue conflatingQueue = new ConflatingQueue(1000000);
        MarketDataGenerator marketDataGenerator = new MarketDataGenerator(9, 10);
        MarketDataProcessor marketDataProcessor = new MarketDataProcessor(marketDataGenerator, conflatingQueue);

        slidingWindow = new SlidingWindow(100, 1); //i.e. 100 requests per second

        marketDataGenerator = new MarketDataGenerator(500, 10);
        marketDataGenerator.generateMockMarketData().forEach(marketDataProcessor::onMessage);
        assertEquals(500, marketDataProcessor.getConflatingQueue().getNoOfSymbols(), "500 market data events created");

        ThrottledPublisher throttledPublisher = new ThrottledPublisher(conflatingQueue, marketDataProcessor, slidingWindow);
        throttledPublisher.publishData();

        assertEquals("{A0=1, A1=1, A10=1, A11=1, A12=1, A13=1, A14=1, A15=1, A16=1, A17=1, A18=1, A19=1, A2=1, A20=1, A21=1, A22=1, A23=1, A24=1, A25=1, A26=1, A27=1, A28=1, A29=1, A3=1, A30=1, A31=1, A32=1, A33=1, A34=1, A35=1, A36=1, A37=1, A38=1, A39=1, A4=1, A40=1, A41=1, A42=1, A43=1, A44=1, A45=1, A46=1, A47=1, A48=1, A49=1, A5=1, A50=1, A51=1, A52=1, A53=1, A54=1, A55=1, A56=1, A57=1, A58=1, A59=1, A6=1, A60=1, A61=1, A62=1, A63=1, A64=1, A65=1, A66=1, A67=1, A68=1, A69=1, A7=1, A70=1, A71=1, A72=1, A73=1, A74=1, A75=1, A76=1, A77=1, A78=1, A79=1, A8=1, A80=1, A81=1, A82=1, A83=1, A84=1, A85=1, A86=1, A87=1, A88=1, A89=1, A9=1, A90=1, A91=1, A92=1, A93=1, A94=1, A95=1, A96=1, A97=1, A98=1, A99=1}",
                throttledPublisher.getPublishCounts().toString(),"Verified that each symbol getting published only once");
        assertEquals(100, throttledPublisher.getPublishCounts().keySet().size());
    }
}
