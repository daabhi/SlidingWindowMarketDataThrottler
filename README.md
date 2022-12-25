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

### Points to consider
1) The marketDataThread is scheduled to generate random 1000 market data records for configured 10 symbols every millisecond, 
which effectively means we are getting 10K updates per millisecond or 10 million updates per second.
        publisherExecutor.scheduleAtFixedRate(() -> marketDataGenerator.generateMockMarketData().forEach(marketDataProcessor::onMessage), 0, 1, TimeUnit.MILLISECONDS);
2) The consumer executor runs every 100 millisecs to check the conflation queue, which means it will check 10 times roughly per second and try to publish the updates 
as per the sliding window and symbol publishing rules.
        consumerExecutor.scheduleAtFixedRate(throttledPublisher::publishData, 0, 100, TimeUnit.MILLISECONDS);

##Sample log. The actual running application log is checked in as well for easier reference.

As we can see that while the producer is taking a break to give the throttled publisher some breathing space, the sliding window size starts shrinking as expected.

2022-12-25 22:31:26,624 [market-data-processor-thread-0] INFO  (MarketDataThrottleApplication.java:36) - Giving the consumer some breathing space for 5 secs. Have published 100 ticks so far
2022-12-25 22:31:27,135 [throttled-publisher-thread-0] INFO  (ThrottledPublisher.java:48) - publishAggregatedMarketData: MarketData(updateTime=2022-12-25T14:31:26.646386Z, symbol=A2, price=Price(bid=1100.0, ask=2100.0, last=1099.0)) publishCounts={A0=5, A1=4, A2=5, A3=4, A4=4, A5=4, A6=4, A7=4, A8=4, A9=4} currentSlidingWindowSize=91
2022-12-25 22:31:27,136 [throttled-publisher-thread-0] INFO  (ThrottledPublisher.java:48) - publishAggregatedMarketData: MarketData(updateTime=2022-12-25T14:31:26.646386Z, symbol=A3, price=Price(bid=1100.0, ask=2100.0, last=1099.0)) publishCounts={A0=5, A1=4, A2=5, A3=5, A4=4, A5=4, A6=4, A7=4, A8=4, A9=4} currentSlidingWindowSize=92
2022-12-25 22:31:27,136 [throttled-publisher-thread-0] INFO  (ThrottledPublisher.java:48) - publishAggregatedMarketData: MarketData(updateTime=2022-12-25T14:31:26.646387Z, symbol=A4, price=Price(bid=1100.0, ask=2100.0, last=1099.0)) publishCounts={A0=5, A1=4, A2=5, A3=5, A4=5, A5=4, A6=4, A7=4, A8=4, A9=4} currentSlidingWindowSize=93
2022-12-25 22:31:27,137 [throttled-publisher-thread-0] INFO  (ThrottledPublisher.java:48) - publishAggregatedMarketData: MarketData(updateTime=2022-12-25T14:31:26.646387Z, symbol=A5, price=Price(bid=1100.0, ask=2100.0, last=1099.0)) publishCounts={A0=5, A1=4, A2=5, A3=5, A4=5, A5=5, A6=4, A7=4, A8=4, A9=4} currentSlidingWindowSize=94
2022-12-25 22:31:27,137 [throttled-publisher-thread-0] INFO  (ThrottledPublisher.java:48) - publishAggregatedMarketData: MarketData(updateTime=2022-12-25T14:31:26.646387Z, symbol=A6, price=Price(bid=1100.0, ask=2100.0, last=1099.0)) publishCounts={A0=5, A1=4, A2=5, A3=5, A4=5, A5=5, A6=5, A7=4, A8=4, A9=4} currentSlidingWindowSize=95
2022-12-25 22:31:27,137 [throttled-publisher-thread-0] INFO  (ThrottledPublisher.java:48) - publishAggregatedMarketData: MarketData(updateTime=2022-12-25T14:31:26.646387Z, symbol=A8, price=Price(bid=1100.0, ask=2100.0, last=1099.0)) publishCounts={A0=5, A1=4, A2=5, A3=5, A4=5, A5=5, A6=5, A7=4, A8=5, A9=4} currentSlidingWindowSize=96
2022-12-25 22:31:27,137 [throttled-publisher-thread-0] INFO  (ThrottledPublisher.java:48) - publishAggregatedMarketData: MarketData(updateTime=2022-12-25T14:31:26.646387Z, symbol=A9, price=Price(bid=1100.0, ask=2100.0, last=1099.0)) publishCounts={A0=5, A1=4, A2=5, A3=5, A4=5, A5=5, A6=5, A7=4, A8=5, A9=5} currentSlidingWindowSize=97
2022-12-25 22:31:27,138 [throttled-publisher-thread-0] INFO  (ThrottledPublisher.java:48) - publishAggregatedMarketData: MarketData(updateTime=2022-12-25T14:31:26.646386Z, symbol=A1, price=Price(bid=1100.0, ask=2100.0, last=1099.0)) publishCounts={A0=5, A1=5, A2=5, A3=5, A4=5, A5=5, A6=5, A7=4, A8=5, A9=5} currentSlidingWindowSize=98
2022-12-25 22:31:27,138 [throttled-publisher-thread-0] INFO  (ThrottledPublisher.java:56) - No of symbols successfully published in this iteration=8
2022-12-25 22:31:27,537 [throttled-publisher-thread-0] INFO  (ThrottledPublisher.java:48) - publishAggregatedMarketData: MarketData(updateTime=2022-12-25T14:31:26.646386Z, symbol=A0, price=Price(bid=1100.0, ask=2100.0, last=1099.0)) publishCounts={A0=6, A1=5, A2=5, A3=5, A4=5, A5=5, A6=5, A7=4, A8=5, A9=5} currentSlidingWindowSize=67
2022-12-25 22:31:27,537 [throttled-publisher-thread-0] INFO  (ThrottledPublisher.java:56) - No of symbols successfully published in this iteration=1
2022-12-25 22:31:31,732 [throttled-publisher-thread-0] INFO  (ThrottledPublisher.java:48) - publishAggregatedMarketData: MarketData(updateTime=2022-12-25T14:31:23.678945Z, symbol=A0, price=Price(bid=514.0, ask=980.0, last=513.0)) publishCounts={A0=7, A1=5, A2=5, A3=5, A4=5, A5=5, A6=5, A7=4, A8=5, A9=5} currentSlidingWindowSize=1
2022-12-25 22:31:31,733 [throttled-publisher-thread-0] INFO  (ThrottledPublisher.java:48) - publishAggregatedMarketData: MarketData(updateTime=2022-12-25T14:31:23.716157Z, symbol=A1, price=Price(bid=283.0, ask=517.0, last=282.0)) publishCounts={A0=7, A1=6, A2=5, A3=5, A4=5, A5=5, A6=5, A7=4, A8=5, A9=5} currentSlidingWindowSize=2
2022-12-25 22:31:31,733 [throttled-publisher-thread-0] INFO  (ThrottledPublisher.java:48) - publishAggregatedMarketData: MarketData(updateTime=2022-12-25T14:31:23.716670Z, symbol=A2, price=Price(bid=721.0, ask=1393.0, last=720.0)) publishCounts={A0=7, A1=6, A2=6, A3=5, A4=5, A5=5, A6=5, A7=4, A8=5, A9=5} currentSlidingWindowSize=3
2022-12-25 22:31:31,734 [throttled-publisher-thread-0] INFO  (ThrottledPublisher.java:48) - publishAggregatedMarketData: MarketData(updateTime=2022-12-25T14:31:23.774964Z, symbol=A3, price=Price(bid=89.0, ask=128.0, last=88.0)) publishCounts={A0=7, A1=6, A2=6, A3=6, A4=5, A5=5, A6=5, A7=4, A8=5, A9=5} currentSlidingWindowSize=4
2022-12-25 22:31:31,734 [throttled-publisher-thread-0] INFO  (ThrottledPublisher.java:48) - publishAggregatedMarketData: MarketData(updateTime=2022-12-25T14:31:23.775609Z, symbol=A4, price=Price(bid=396.0, ask=742.0, last=395.0)) publishCounts={A0=7, A1=6, A2=6, A3=6, A4=6, A5=5, A6=5, A7=4, A8=5, A9=5} currentSlidingWindowSize=5
