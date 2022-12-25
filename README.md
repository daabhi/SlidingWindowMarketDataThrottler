##Key Requirements
/**
*  At least fulfill,
*  Ensure that the number of calls of publishAggregatedMarketData method for publishing messages does not exceed 100 times per second, where this period is a sliding window.
*  Ensure that each symbol does not update more than once per sliding window.
* o Prefer to fulfill,
*  Ensure that each symbol always has the latest market data published.
*  Ensure the latest market data on each symbol will be published.
  */

###SlidingWindowMarketDataThrottler##

1) ConflationQueue maintains the latest market data per symbol and also keeps a list of pending symbols to be published in a throttled fashion
2) MarketDataProcessor gets a callback onMessage where on receiving the data it simply puts it into conflation queue.
3) ThrottledPublisher keeps polling the conflation queue for messages 
   1) Whatever it is able to publish based on the 2 conditions of 100 mps and not more than 1 symbol per sliding window it removes from pending symbols 
   2) For the rest of symbols which it could not publish because of failing above 2 conditions it waits for the next iteration.
4) As conflation queue always has latest data, so it is ensured that marketData being published for a symbol is always the latest.
5) MarketDataThrottleApplication is the main entry point of the application
6) Appropriate tests are added for the sliding window and throttled publisher, but more cases can be added for a real production usage.
7) No validation checks for one sided book, 0 price, negative price or quantity etc etc data quality checks are not added as it is assumed that this process is just meant to publish the latest in the fastest way possible. Client using this should perform these checks.
8) MarketData would have last quantity etc but in the interest of this exercise, as mentioned only price is added.