package throttler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SlidingWindowTest {
    private SlidingWindow slidingWindow = new SlidingWindow(3,1);//3 requests per second
    @Test
    public void testVerifySlidingWindowBlocksDueToNoOfRequestPerSecBlock() throws InterruptedException {
        assertTrue(slidingWindow.isAllowed());
        assertTrue(slidingWindow.isAllowed());
        assertTrue(slidingWindow.isAllowed());
        assertFalse(slidingWindow.isAllowed());

        Thread.sleep(1000);
        assertTrue(slidingWindow.isAllowed());
        assertTrue(slidingWindow.isAllowed());
        assertTrue(slidingWindow.isAllowed());
        assertFalse(slidingWindow.isAllowed());

    }
    @Test
    public void testVerifySlidingWindowBlockDueToSymbolNotAllowedToBePublishedMoreThanOncePerSlidingWindow() throws InterruptedException {
        assertTrue(slidingWindow.shouldPublishSymbolWithinWindow("A"));
        assertFalse(slidingWindow.shouldPublishSymbolWithinWindow("A"));
        assertTrue(slidingWindow.shouldPublishSymbolWithinWindow("B"));
        Thread.sleep(1000);
        assertTrue(slidingWindow.shouldPublishSymbolWithinWindow("A"));
    }

    @Test
    public void testVerify4thMsgBlockedDueToNoOfRequests(){
        assertTrue(slidingWindow.canPublish("A"));
        assertTrue(slidingWindow.canPublish("B"));
        assertTrue(slidingWindow.canPublish("C"));
        assertFalse(slidingWindow.canPublish("D"));//4th request is blocked as anyways 3 requests per second allowed
    }
}
