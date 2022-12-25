package interfaces;

import pojo.MarketData;

public interface IMarketDataProcessor {
    void publishAggregatedMarketData(MarketData data);
    void onMessage(MarketData data);
}
