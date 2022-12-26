package throttler;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import interfaces.IMarketDataProcessor;
import interfaces.IThrottledPublisher;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import pojo.ConflatingQueue;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class MarketDataThrottleApplication {
    private static final Logger logger = LogManager.getLogger(MarketDataThrottleApplication.class.getSimpleName());
    private static final int maxRequestPerInterval        = 100;
    private static final long maxIntervalInSecs           = 1;
    private static final int noOfSymbols                  = 10;
    private static final int noOfRecordsPerSymbol         = 1000;
    private static final ConflatingQueue conflatingQueue  = new ConflatingQueue(1000000);
    private static final SlidingWindow slidingWindow      = new SlidingWindow(maxRequestPerInterval, maxIntervalInSecs);
    private static final ScheduledExecutorService publisherExecutor         = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("market-data-processor-thread-%d").build());
    private static final ScheduledExecutorService consumerExecutor         = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("throttled-publisher-thread-%d").build());

    public static void main(String[] args) {
        MarketDataGenerator marketDataGenerator   = new MarketDataGenerator(noOfSymbols, noOfRecordsPerSymbol);
        IMarketDataProcessor marketDataProcessor  = new MarketDataProcessor(marketDataGenerator, conflatingQueue);
        IThrottledPublisher throttledPublisher    = new ThrottledPublisher(conflatingQueue, marketDataProcessor, slidingWindow);

        AtomicInteger count = new AtomicInteger(1);
        publisherExecutor.scheduleAtFixedRate(() -> {

            marketDataGenerator.generateMockMarketData().forEach(marketDataProcessor::onMessage);
            count.incrementAndGet();
            if(count.get()%100==0){
                logger.info("Giving the consumer some breathing space for 5 secs. Have published "+ count + " ticks so far");
                try {
                    Thread.sleep(5000);//There is no reason to add Thread.sleep here but just want to highlight in the logs that the sliding window shrinks in the throttled publisher when the producer is giving the throttled publisher breathing space
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1, TimeUnit.MILLISECONDS);
        consumerExecutor.scheduleAtFixedRate(throttledPublisher::publishData, 0, 100, TimeUnit.MILLISECONDS);
    }
}

