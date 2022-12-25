package throttler;

import interfaces.IMarketDataProcessor;
import interfaces.IThrottledPublisher;
import pojo.ConflatingQueue;

import java.util.concurrent.*;


public class MarketDataThrottleApplication {
    private static final int maxRequestPerInterval        = 100;
    private static final long maxIntervalInSecs           = 1;
    private static final int noOfSymbols                  = 10;
    private static final int noOfRecordsPerSymbol         = 1000;
    private static final ConflatingQueue conflatingQueue  = new ConflatingQueue(1000000);
    private static final SlidingWindow slidingWindow      = new SlidingWindow(maxRequestPerInterval, maxIntervalInSecs);
    private static final ScheduledExecutorService publisherExecutor         = Executors.newSingleThreadScheduledExecutor();
    private static final ScheduledExecutorService consumerExecutor         = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) {
        MarketDataGenerator marketDataGenerator   = new MarketDataGenerator(noOfSymbols, noOfRecordsPerSymbol);
        IMarketDataProcessor marketDataProcessor  = new MarketDataProcessor(marketDataGenerator, conflatingQueue);
        IThrottledPublisher throttledPublisher    = new ThrottledPublisher(conflatingQueue, marketDataProcessor, slidingWindow);

        publisherExecutor.scheduleAtFixedRate(() -> marketDataGenerator.generateMockMarketData().forEach(marketDataProcessor::onMessage), 0, 10, TimeUnit.MILLISECONDS);
        consumerExecutor.scheduleAtFixedRate(throttledPublisher::publishData, 0, 100, TimeUnit.MILLISECONDS);
    }
}

