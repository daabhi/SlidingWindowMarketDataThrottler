package throttler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SlidingWindowTest {
    private SlidingWindow slidingWindow = new SlidingWindow(3,1);//3 requests per second
    @Test
    public void testVerifySlidingWindowBlocksDueToNoOfRequestPerSecBlock() throws InterruptedException {
        assertEquals(true,slidingWindow.isAllowed());
        assertEquals(true,slidingWindow.isAllowed());
        assertEquals(true,slidingWindow.isAllowed());
        assertEquals(false,slidingWindow.isAllowed());

        Thread.sleep(1000);
        assertEquals(true,slidingWindow.isAllowed());
        assertEquals(true,slidingWindow.isAllowed());
        assertEquals(true,slidingWindow.isAllowed());
        assertEquals(false,slidingWindow.isAllowed());

    }
    @Test
    public void testVerifySlidingWindowBlockDueToSymbolNotAllowedToBePublishedMoreThanOncePerSlidingWindow(){
        assertEquals(true,slidingWindow.shouldPublishSymbolWithinWindow("A"));
        assertEquals(false,slidingWindow.shouldPublishSymbolWithinWindow("A"));
    }

    @Test
    public void testVerify4thMsgBlockedDueToNoOfRequests(){
        assertEquals(true,slidingWindow.canPublish("A"));
        assertEquals(true,slidingWindow.canPublish("B"));
        assertEquals(true,slidingWindow.canPublish("C"));
        assertEquals(false,slidingWindow.canPublish("D"));
    }
}
