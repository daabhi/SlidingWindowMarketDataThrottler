package throttler;

import interfaces.IMarketDataProcessor;
import interfaces.IThrottledPublisher;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import pojo.ConflatingQueue;
import pojo.MarketData;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Getter @Setter @ToString
public class ThrottledPublisher implements IThrottledPublisher {
    private static final Logger logger = LogManager.getLogger(ThrottledPublisher.class.getSimpleName());
    private final ConflatingQueue conflatingQueue;
    private final IMarketDataProcessor marketDataProcessor;
    private final SlidingWindow         slidingWindow;
    private final Map<String, Integer>  publishCounts = new TreeMap<>();

    public ThrottledPublisher(ConflatingQueue conflatingQueue, IMarketDataProcessor marketDataProcessor, SlidingWindow slidingWindow) {
        this.conflatingQueue     = conflatingQueue;
        this.marketDataProcessor = marketDataProcessor;
        this.slidingWindow       = slidingWindow;
    }

    /**
     * This function can get called periodically or whenever there is data to be published, whatever it can publish out of the pending symbols to published it will do, rest it will leave for next iteration.
     */
    @Override
    public void publishData() {
        if (conflatingQueue.isEmpty()) return;
        Set<String> pendingSymbolsForPublishing = conflatingQueue.getPendingSymbolsForPublishing();
        int origNoOfPendingSymbols = pendingSymbolsForPublishing.size();
        int counter = 0, successfulCounter=0;
        while (!pendingSymbolsForPublishing.isEmpty()) {
            MarketData marketData = conflatingQueue.peek();
            counter++;
            if (marketData != null) {
                String symbol = marketData.getSymbol();
                if (slidingWindow.canPublish(symbol)) {
                    publishCounts.put(symbol, publishCounts.getOrDefault(symbol, 0) + 1);
                    marketDataProcessor.publishAggregatedMarketData(marketData); //Ideally if the return type is boolean, then only after we successfully publish the aggregated data, we must remove from pendingSymbols and conflation queue.
                    logger.info("publishAggregatedMarketData: " + marketData +" publishCounts="+ publishCounts + " currentSlidingWindowSize="+ slidingWindow.getSize());
                    pendingSymbolsForPublishing.remove(symbol);
                    successfulCounter++;
                    conflatingQueue.take();
                }
            }
            if (counter == origNoOfPendingSymbols) {//Tried all the pending symbols in this iteration and published whatever it could, will leave the rest for next iteration
                break;
            }
        }
        if(successfulCounter>0) {
            logger.debug("No of symbols successfully published in this iteration=" + successfulCounter);
        }
    }
}
