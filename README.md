## Key Requirements

### Atleast fulfill

#### 1. Ensure that the number of calls of publishAggregatedMarketData method for publishing messages does not exceed 100 times per second, where this period is a sliding window.
#### 2. Ensure that each symbol does not update more than once per sliding window.

### Prefer to fulfill
#### 1. Ensure that each symbol always has the latest market data published.
#### 2. Ensure the latest market data on each symbol will be published.

### Implementation Choices


#### Option 1(Simply trying to throttle the updates as it gets the market data update) - Rejected due to below reasons
1. We can go for a simple approach for simply pushing the market data to the throttled publisher when the market data thread and then the throttled publisher can simply run its logic and decide whether it can publish it or not based on sliding window or symbol block
2. The problem with the simplistic approach in point 1 is that it is possible that at the start of the second we get a junk update for a symbol and then we publish it, if we drop the second update for the symbol which possibly comes at later part of second then we will get stuck if the good update for that symbol does not come in the subsequent second.
3. Another problem with the approach in point 1 is that for illiquid symbols if we get an update as the 101st msg in this example and we drop it and we don't receive another update in the next 5 mins as illiquid stock, then we will never publish that update of the illiquid stock in time

#### Option 2(Usage of conflation queue for maintaining latest market data snapshot and pending symbols to be published concurrent list in snapshot order to ensure latest market data gets published in the same order for the latest symbol) 
1. The inherent nature of this issue is that market data is a conflatable event as we normally care about latest snapshot and hence conflation queue is a good candidate.
2. We keep in mind that if it was a normal request throttling then if the sliding window or symbol breached we could simply reject the request and expect client to retry but given our current need we cannot simply reject and hence the need to have a store in place.

#### SlidingWindowMarketDataThrottler implementation details

1) MarketDataThrottleApplication is the main entry point of the application
2) ConflationQueue maintains the latest market data per symbol and also keeps a list of pending symbols to be published in a throttled fashion
3) MarketDataProcessor gets a callback onMessage where on receiving the data it simply puts it into conflation queue(Internally conflation queue maintains latest snapshot per symbol in a map, list of symbols and a pending insertion order maintained list of symbols for the throttled publisher to pull)
4) ThrottledPublisher keeps polling the conflation queue for messages 10 times per second or every 100 millisecs.
   1) Whatever it is able to publish based on the 2 conditions of 100 mps and not more than 1 symbol per sliding window it removes from pending symbols 
   2) For the rest of symbols which it could not publish because of failing above 2 conditions it waits for the next iteration.
5) As conflation queue always has latest data for every symbol in insertion order maintained fashion, so it is ensured that marketData being published for a symbol is always the latest.
6) Appropriate tests are added for the sliding window, conflation queue and throttled publisher, but more cases should be added for a real production usage.
7) No validation checks for one sided book, 0 price, negative price or quantity etc etc data quality checks are not added as it is assumed that this process is just meant to publish the latest in the fastest way possible. Client using this should perform these checks.
8) MarketData would have last quantity etc but in the interest of this exercise, as mentioned only price is added.

#### Points to note
1) The marketDataThread is scheduled to generate random 1000 market data records for configured 10 symbols every millisecond, 
which effectively means we are getting 10K updates per millisecond or 10 million updates per second.
        publisherExecutor.scheduleAtFixedRate(() -> marketDataGenerator.generateMockMarketData().forEach(marketDataProcessor::onMessage), 0, 1, TimeUnit.MILLISECONDS);
2) The consumer executor runs every 100 millisecs to check the conflation queue, which means it will check 10 times roughly per second and try to publish the updates 
as per the sliding window and symbol publishing rules.
        consumerExecutor.scheduleAtFixedRate(throttledPublisher::publishData, 0, 100, TimeUnit.MILLISECONDS);

