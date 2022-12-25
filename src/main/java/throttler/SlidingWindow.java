package throttler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class SlidingWindow {
    private final Queue<Long> slidingWindow = new LinkedList<>();
    private final int maxRequestPerInterval ;
    private final long timeIntervalInMillisecs;
    private final HashMap<String, Long> symbolLastPublishTimes = new HashMap<>();
    public SlidingWindow(int maxRequestPerInterval, long intervalInSecs){
        this.maxRequestPerInterval = maxRequestPerInterval;
        this.timeIntervalInMillisecs = intervalInSecs*1000;
    }

    public int getSize(){
        return slidingWindow.size();
    }

    public boolean canPublish(String symbol){
        return isAllowed() && shouldPublishSymbolWithinWindow(symbol);
    }

    /**
     * It tries to remove all the timestamps which are older than the time interval and compares that size with the max rps for verifying eligibility
     * @return
     */
    public boolean isAllowed() {
        long now = System.currentTimeMillis();
        while (!slidingWindow.isEmpty() && slidingWindow.element() <= now-timeIntervalInMillisecs) {
            slidingWindow.poll();
        }
        slidingWindow.add(now);
        return slidingWindow.size() <= maxRequestPerInterval;
    }

    /**
     * Runs the check to make sure in every sliding window, each symbol is published once.
     * @param symbol
     * @return
     */
    public boolean shouldPublishSymbolWithinWindow(String symbol) {
        long curTime              = System.currentTimeMillis();
        Long lastPublishedInstant = symbolLastPublishTimes.get(symbol);
        if (lastPublishedInstant == null) {
            symbolLastPublishTimes.put(symbol, curTime);
            return true;
        }
        long timeElapsed = curTime - lastPublishedInstant;
        if (timeElapsed >= timeIntervalInMillisecs) {
            symbolLastPublishTimes.put(symbol, curTime);
            return true;
        }
        return false;
    }
}
