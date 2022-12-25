package throttler;

import interfaces.IMarketDataProcessor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import pojo.ConflatingQueue;
import pojo.MarketData;


/**
 *  At least fulfill,
 *  Ensure that the number of calls of publishAggregatedMarketData method for publishing messages does not exceed 100 times per second, where this period is a sliding window.
 *  Ensure that each symbol does not update more than once per sliding window.
 * o Prefer to fulfill,
 *  Ensure that each symbol always has the latest market data published.
 *  Ensure the latest market data on each symbol will be published. At least fulfill,
 */
@Getter @Setter @EqualsAndHashCode @ToString
public class MarketDataProcessor implements IMarketDataProcessor {
    private static final Logger logger = LogManager.getLogger(MarketDataProcessor.class.getSimpleName());

    private MarketDataGenerator marketDataGenerator;
    private ConflatingQueue conflatingQueue;

    public MarketDataProcessor(MarketDataGenerator marketDataGenerator, ConflatingQueue conflatingQueue) {
        this.marketDataGenerator = marketDataGenerator;
        this.conflatingQueue     = conflatingQueue;
    }

    public void publishAggregatedMarketData(MarketData data) {
        // For test do Nothing, assume implemented.
    }

    public void onMessage(MarketData data) {
        conflatingQueue.offer(data);
    }
}

