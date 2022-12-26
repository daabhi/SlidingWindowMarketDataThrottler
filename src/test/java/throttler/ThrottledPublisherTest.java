package throttler;

import org.junit.jupiter.api.Test;
import pojo.ConflatingQueue;
import pojo.MarketData;
import pojo.Price;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ThrottledPublisherTest {

    /**
     * 1. Producer publishes 10 records each for 9 symbols at one instant which end up getting conflated in our queue(latest overrides it)
     * 2. throttler.SlidingWindow allows only 3 requests per second
     * 3. We assert that only 3 messages are sent in 1st sec, followed by 3 in each subsequent second.
     */
    @Test
    public void testPublish() throws InterruptedException {
        SlidingWindow slidingWindow = new SlidingWindow(3, 1); //i.e. 3 requests per second
        ConflatingQueue conflatingQueue = new ConflatingQueue(1000000);
        MarketDataGenerator marketDataGenerator = new MarketDataGenerator(9, 10);
        MarketDataProcessor marketDataProcessor = new MarketDataProcessor(marketDataGenerator, conflatingQueue);

        marketDataGenerator.generateMockMarketData().forEach(marketDataProcessor::onMessage);
        assertEquals(9, marketDataProcessor.getConflatingQueue().getNoOfSymbols(), "9 market data events created");

        ThrottledPublisher throttledPublisher = new ThrottledPublisher(conflatingQueue, marketDataProcessor, slidingWindow);
        throttledPublisher.publishData();

        assertEquals("{A0=1, A1=1, A2=1}", throttledPublisher.getPublishCounts().toString());
        //assertEquals("[A3, A4, A5, A6, A7, A8]", throttledPublisher.getPendingQueue().stream().map(pojo.MarketData::getSymbol).collect(Collectors.toList()).toString(), "6 messages throttled as expected, only 3 allowed per second");

        Thread.sleep(1000);
        throttledPublisher.publishData();
        assertEquals("{A0=1, A1=1, A2=1, A3=1, A4=1, A5=1}", throttledPublisher.getPublishCounts().toString(), "6 messages have been published, after 2 secs");

        Thread.sleep(1000);
        throttledPublisher.publishData();
        assertEquals("{A0=1, A1=1, A2=1, A3=1, A4=1, A5=1, A6=1, A7=1, A8=1}", throttledPublisher.getPublishCounts().toString(), "9 messages have been published after 3 secs");
    }

    /**
     * Verify that the conflation logic works as expected to ensure our queue always has the latest market data.
     */
    @Test
    public void testMarketDataGenerationFollowedByConflation() {
        SlidingWindow slidingWindow = new SlidingWindow(3, 1); //i.e. 3 requests per second
        ConflatingQueue conflatingQueue = new ConflatingQueue(1000000);
        MarketDataGenerator marketDataGenerator = new MarketDataGenerator(9, 10);
        MarketDataProcessor marketDataProcessor = new MarketDataProcessor(marketDataGenerator, conflatingQueue);


        marketDataGenerator = new MarketDataGenerator(2, 2);
        List<MarketData> marketData = new ArrayList<>(marketDataGenerator.generateMockMarketData());
        assertEquals(4, marketData.size());

        assertEquals("A0", marketData.get(0).getSymbol());
        assertEquals("A1", marketData.get(1).getSymbol());
        assertEquals("A0", marketData.get(2).getSymbol());
        assertEquals("A1", marketData.get(3).getSymbol());

        assertEquals("Price(bid=4.0, ask=5.0, last=3.0)", marketData.get(0).getPrice().toString());
        assertEquals("Price(bid=4.0, ask=5.0, last=3.0)", marketData.get(1).getPrice().toString());
        assertEquals("Price(bid=5.0, ask=7.0, last=4.0)", marketData.get(2).getPrice().toString());
        assertEquals("Price(bid=5.0, ask=7.0, last=4.0)", marketData.get(3).getPrice().toString());

        marketData.forEach(marketDataProcessor::onMessage);
        assertEquals(2, marketDataProcessor.getConflatingQueue().getNoOfSymbols());
        Queue<String> symbols = marketDataProcessor.getConflatingQueue().getSymbols();

        assertEquals("A0", marketDataProcessor.getConflatingQueue().getMarketData("A0").getSymbol(),"A0 has latest price of 5,7,4 instead of 4,5,3 as expected");
        assertEquals("Price(bid=5.0, ask=7.0, last=4.0)", marketDataProcessor.getConflatingQueue().getMarketData("A1").getPrice().toString(),"A0 has latest price of 5,7,4 instead of 4,5,3 as expected");

        assertEquals("A1", marketDataProcessor.getConflatingQueue().getMarketData("A1").getSymbol());
        assertEquals("Price(bid=5.0, ask=7.0, last=4.0)", marketDataProcessor.getConflatingQueue().getMarketData("A1").getPrice().toString());
    }

    @Test
    public void testEachSymbolGetsPublishedOnlyOncePerSlidingWindow() {
        SlidingWindow slidingWindow = new SlidingWindow(3, 1); //i.e. 3 requests per second
        ConflatingQueue conflatingQueue = new ConflatingQueue(1000000);
        MarketDataGenerator marketDataGenerator = new MarketDataGenerator(4, 10);
        MarketDataProcessor marketDataProcessor = new MarketDataProcessor(marketDataGenerator, conflatingQueue);

        marketDataGenerator.generateMockMarketData().forEach(marketDataProcessor::onMessage);
        assertEquals(4, marketDataProcessor.getConflatingQueue().getNoOfSymbols(), "40 market data events created");

        ThrottledPublisher throttledPublisher = new ThrottledPublisher(conflatingQueue, marketDataProcessor, slidingWindow);
        throttledPublisher.publishData();

        assertEquals("{A0=1, A1=1, A2=1}", throttledPublisher.getPublishCounts().toString(),"Verified that each symbol getting published only once");
    }

    @Test
    public void testNoOfRecordsPublishedDoesNotExceed100PerSlidingWindow(){
        ConflatingQueue conflatingQueue = new ConflatingQueue(1000000);
        MarketDataGenerator marketDataGenerator = new MarketDataGenerator(500, 10);
        MarketDataProcessor marketDataProcessor = new MarketDataProcessor(marketDataGenerator, conflatingQueue);

        SlidingWindow slidingWindow = new SlidingWindow(100, 1); //i.e. 100 requests per second

        marketDataGenerator.generateMockMarketData().forEach(marketDataProcessor::onMessage);
        assertEquals(500, marketDataProcessor.getConflatingQueue().getNoOfSymbols(), "500 market data events created");

        ThrottledPublisher throttledPublisher = new ThrottledPublisher(conflatingQueue, marketDataProcessor, slidingWindow);
        throttledPublisher.publishData();

        assertEquals("{A0=1, A1=1, A2=1, A3=1, A4=1, A5=1, A6=1, A7=1, A8=1, A9=1, A10=1, A11=1, A12=1, A13=1, A14=1, A15=1, A16=1, A17=1, A18=1, A19=1, A20=1, A21=1, A22=1, A23=1, A24=1, A25=1, A26=1, A27=1, A28=1, A29=1, A30=1, A31=1, A32=1, A33=1, A34=1, A35=1, A36=1, A37=1, A38=1, A39=1, A40=1, A41=1, A42=1, A43=1, A44=1, A45=1, A46=1, A47=1, A48=1, A49=1, A50=1, A51=1, A52=1, A53=1, A54=1, A55=1, A56=1, A57=1, A58=1, A59=1, A60=1, A61=1, A62=1, A63=1, A64=1, A65=1, A66=1, A67=1, A68=1, A69=1, A70=1, A71=1, A72=1, A73=1, A74=1, A75=1, A76=1, A77=1, A78=1, A79=1, A80=1, A81=1, A82=1, A83=1, A84=1, A85=1, A86=1, A87=1, A88=1, A89=1, A90=1, A91=1, A92=1, A93=1, A94=1, A95=1, A96=1, A97=1, " +
                        "A98=1, A99=1}",
                throttledPublisher.getPublishCounts().toString(),"Verified that each symbol getting published only once");
        assertEquals(100, throttledPublisher.getPublishCounts().keySet().size());
    }  
    
    @Test
    public void loadTest() throws InterruptedException {
        ConflatingQueue conflatingQueue = new ConflatingQueue(1000000);
        MarketDataGenerator marketDataGenerator = new MarketDataGenerator(400, 10000);//Generate 4 million events
        MarketDataProcessor marketDataProcessor = new MarketDataProcessor(marketDataGenerator, conflatingQueue);
        SlidingWindow slidingWindow = new SlidingWindow(100, 1); //i.e. 100 requests per second

        marketDataGenerator.generateMockMarketData().forEach(marketDataProcessor::onMessage);
        assertEquals(400, marketDataProcessor.getConflatingQueue().getNoOfSymbols(), "400 market data events created");

        ThrottledPublisher throttledPublisher = new ThrottledPublisher(conflatingQueue, marketDataProcessor, slidingWindow);
        //Assert that it takes 5 iterations across 5 secs to publish all 400 symbols
        Thread.sleep(1000);
        throttledPublisher.publishData();
        assertEquals("{A0=1, A1=1, A2=1, A3=1, A4=1, A5=1, A6=1, A7=1, A8=1, A9=1, A10=1, A11=1, A12=1, A13=1, A14=1, A15=1, A16=1, A17=1, A18=1, A19=1, A20=1, A21=1, A22=1, A23=1, A24=1, A25=1, A26=1, A27=1, A28=1, A29=1, A30=1, A31=1, A32=1, A33=1, A34=1, A35=1, A36=1, A37=1, A38=1, A39=1, A40=1, A41=1, A42=1, A43=1, A44=1, A45=1, A46=1, A47=1, A48=1, A49=1, A50=1, A51=1, A52=1, A53=1, A54=1, A55=1, A56=1, A57=1, A58=1, A59=1, A60=1, A61=1, A62=1, A63=1, A64=1, A65=1, A66=1, A67=1, A68=1, A69=1, A70=1, A71=1, A72=1, A73=1, A74=1, A75=1, A76=1, A77=1, A78=1, A79=1, A80=1, A81=1, A82=1, A83=1, A84=1, A85=1, A86=1, A87=1, A88=1, A89=1, A90=1, A91=1, A92=1, A93=1, A94=1, A95=1, A96=1, A97=1, A98=1, A99=1}", throttledPublisher.getPublishCounts().toString(),"Verified that each symbol getting published only once");
        Thread.sleep(1000);
        throttledPublisher.publishData();
        assertEquals("{A0=1, A1=1, A2=1, A3=1, A4=1, A5=1, A6=1, A7=1, A8=1, A9=1, A10=1, A11=1, A12=1, A13=1, A14=1, A15=1, A16=1, A17=1, A18=1, A19=1, A20=1, A21=1, A22=1, A23=1, A24=1, A25=1, A26=1, A27=1, A28=1, A29=1, A30=1, A31=1, A32=1, A33=1, A34=1, A35=1, A36=1, A37=1, A38=1, A39=1, A40=1, A41=1, A42=1, A43=1, A44=1, A45=1, A46=1, A47=1, A48=1, A49=1, A50=1, A51=1, A52=1, A53=1, A54=1, A55=1, A56=1, A57=1, A58=1, A59=1, A60=1, A61=1, A62=1, A63=1, A64=1, A65=1, A66=1, A67=1, A68=1, A69=1, A70=1, A71=1, A72=1, A73=1, A74=1, A75=1, A76=1, A77=1, A78=1, A79=1, A80=1, A81=1, A82=1, A83=1, A84=1, A85=1, A86=1, A87=1, A88=1, A89=1, A90=1, A91=1, A92=1, A93=1, A94=1, A95=1, A96=1, A97=1, A98=1, A99=1, A100=1, A101=1, A102=1, A103=1, A104=1, A105=1, A106=1, A107=1, A108=1, A109=1, A110=1, A111=1, A112=1, A113=1, A114=1, A115=1, A116=1, A117=1, A118=1, A119=1, A120=1, A121=1, A122=1, A123=1, A124=1, A125=1, A126=1, A127=1, A128=1, A129=1, A130=1, A131=1, A132=1, A133=1, A134=1, A135=1, A136=1, A137=1, A138=1, A139=1, A140=1, A141=1, A142=1, A143=1, A144=1, A145=1, A146=1, A147=1, A148=1, A149=1, A150=1, A151=1, A152=1, A153=1, A154=1, A155=1, A156=1, A157=1, A158=1, A159=1, A160=1, A161=1, A162=1, A163=1, A164=1, A165=1, A166=1, A167=1, A168=1, A169=1, A170=1, A171=1, A172=1, A173=1, A174=1, A175=1, A176=1, A177=1, A178=1, A179=1, A180=1, A181=1, A182=1, A183=1, A184=1, A185=1, A186=1, A187=1, A188=1, A189=1, A190=1, A191=1, A192=1, A193=1, A194=1, A195=1, A196=1," +
                " A197=1, A198=1, A199=1}", throttledPublisher.getPublishCounts().toString(),"Verified that each symbol getting published only once");

        Thread.sleep(1000);
        throttledPublisher.publishData();
        assertEquals("{A0=1, A1=1, A2=1, A3=1, A4=1, A5=1, A6=1, A7=1, A8=1, A9=1, A10=1, A11=1, A12=1, A13=1, A14=1, A15=1, A16=1, A17=1, A18=1, A19=1, A20=1, A21=1, A22=1, A23=1, A24=1, A25=1, A26=1, A27=1, A28=1, A29=1, A30=1, A31=1, A32=1, A33=1, A34=1, A35=1, A36=1, A37=1, A38=1, A39=1, A40=1, A41=1, A42=1, A43=1, A44=1, A45=1, A46=1, A47=1, A48=1, A49=1, A50=1, A51=1, A52=1, A53=1, A54=1, A55=1, A56=1, A57=1, A58=1, A59=1, A60=1, A61=1, A62=1, A63=1, A64=1, A65=1, A66=1, A67=1, A68=1, A69=1, A70=1, A71=1, A72=1, A73=1, A74=1, A75=1, A76=1, A77=1, A78=1, A79=1, A80=1, A81=1, A82=1, A83=1, A84=1, A85=1, A86=1, A87=1, A88=1, A89=1, A90=1, A91=1, A92=1, A93=1, A94=1, A95=1, A96=1, A97=1, A98=1, A99=1, A100=1, A101=1, A102=1, A103=1, A104=1, A105=1, A106=1, A107=1, A108=1, A109=1, A110=1, A111=1, A112=1, A113=1, A114=1, A115=1, A116=1, A117=1, A118=1, A119=1, A120=1, A121=1, A122=1, A123=1, A124=1, A125=1, A126=1, A127=1, A128=1, A129=1, A130=1, A131=1, A132=1, A133=1, A134=1, A135=1, A136=1, A137=1, A138=1, A139=1, A140=1, A141=1, A142=1, A143=1, A144=1, A145=1, A146=1, A147=1, A148=1, A149=1, A150=1, A151=1, A152=1, A153=1, A154=1, A155=1, A156=1, A157=1, A158=1, A159=1, A160=1, A161=1, A162=1, A163=1, A164=1, A165=1, A166=1, A167=1, A168=1, A169=1, A170=1, A171=1, A172=1, A173=1, A174=1, A175=1, A176=1, A177=1, A178=1, A179=1, A180=1, A181=1, A182=1, A183=1, A184=1, A185=1, A186=1, A187=1, A188=1, A189=1, A190=1, A191=1, A192=1, A193=1, A194=1, A195=1, A196=1, A197=1, A198=1, A199=1, A200=1, A201=1, A202=1, A203=1, A204=1, A205=1, A206=1, A207=1, A208=1, A209=1, A210=1, A211=1, A212=1, A213=1, A214=1, A215=1, A216=1, A217=1, A218=1, A219=1, A220=1, A221=1, A222=1, A223=1, A224=1, A225=1, A226=1, A227=1, A228=1, A229=1, A230=1, A231=1, A232=1, A233=1, A234=1, A235=1, A236=1, A237=1, A238=1, A239=1, A240=1, A241=1, A242=1, A243=1, A244=1, A245=1, A246=1, A247=1, A248=1, A249=1, A250=1, A251=1, A252=1, A253=1, A254=1, A255=1, A256=1, A257=1, A258=1, A259=1, A260=1, A261=1, A262=1, A263=1, A264=1, A265=1, A266=1, A267=1, A268=1, A269=1, A270=1, A271=1, A272=1, " +
                "A273=1, A274=1, A275=1, A276=1, A277=1, A278=1, A279=1, A280=1, A281=1, A282=1, A283=1, A284=1, A285=1, A286=1, A287=1, A288=1, A289=1, A290=1, A291=1, A292=1, A293=1, A294=1, A295=1, A296=1, A297=1, A298=1, A299=1}", throttledPublisher.getPublishCounts().toString(),"Verified that each symbol getting published only once");
        Thread.sleep(1000);
        throttledPublisher.publishData();
        assertEquals("{A0=1, A1=1, A2=1, A3=1, A4=1, A5=1, A6=1, A7=1, A8=1, A9=1, A10=1, A11=1, A12=1, A13=1, A14=1, A15=1, A16=1, A17=1, A18=1, A19=1, A20=1, A21=1, A22=1, A23=1, A24=1, A25=1, A26=1, A27=1, A28=1, A29=1, A30=1, A31=1, A32=1, A33=1, A34=1, A35=1, A36=1, A37=1, A38=1, A39=1, A40=1, A41=1, A42=1, A43=1, A44=1, A45=1, A46=1, A47=1, A48=1, A49=1, A50=1, A51=1, A52=1, A53=1, A54=1, A55=1, A56=1, A57=1, A58=1, A59=1, A60=1, A61=1, A62=1, A63=1, A64=1, A65=1, A66=1, A67=1, A68=1, A69=1, A70=1, A71=1, A72=1, A73=1, A74=1, A75=1, A76=1, A77=1, A78=1, A79=1, A80=1, A81=1, A82=1, A83=1, A84=1, A85=1, A86=1, A87=1, A88=1, A89=1, A90=1, A91=1, A92=1, A93=1, A94=1, A95=1, A96=1, A97=1, A98=1, A99=1, A100=1, A101=1, A102=1, A103=1, A104=1, A105=1, A106=1, A107=1, A108=1, A109=1, A110=1, A111=1, A112=1, A113=1, A114=1, A115=1, A116=1, A117=1, A118=1, A119=1, A120=1, A121=1, A122=1, A123=1, A124=1, A125=1, A126=1, A127=1, A128=1, A129=1, A130=1, A131=1, A132=1, A133=1, A134=1, A135=1, A136=1, A137=1, A138=1, A139=1, A140=1, A141=1, A142=1, A143=1, A144=1, A145=1, A146=1, A147=1, A148=1, A149=1, A150=1, A151=1, A152=1, A153=1, A154=1, A155=1, A156=1, A157=1, A158=1, A159=1, A160=1, A161=1, A162=1, A163=1, A164=1, A165=1, A166=1, A167=1, A168=1, A169=1, A170=1, A171=1, A172=1, A173=1, A174=1, A175=1, A176=1, A177=1, A178=1, A179=1, A180=1, A181=1, A182=1, A183=1, A184=1, A185=1, A186=1, A187=1, A188=1, A189=1, A190=1, A191=1, A192=1, A193=1, A194=1, A195=1, A196=1, A197=1, A198=1, A199=1, A200=1, A201=1, A202=1, A203=1, A204=1, A205=1, A206=1, A207=1, A208=1, A209=1, A210=1, A211=1, A212=1, A213=1, A214=1, A215=1, A216=1, A217=1, A218=1, A219=1, A220=1, A221=1, A222=1, A223=1, A224=1, A225=1, A226=1, A227=1, A228=1, A229=1, A230=1, A231=1, A232=1, A233=1, A234=1, A235=1, A236=1, A237=1, A238=1, A239=1, A240=1, A241=1, A242=1, A243=1, A244=1, A245=1, A246=1, A247=1, A248=1, A249=1, A250=1, A251=1, A252=1, A253=1, A254=1, A255=1, A256=1, A257=1, A258=1, A259=1, A260=1, A261=1, A262=1, A263=1, A264=1, A265=1, A266=1, A267=1, A268=1, A269=1, A270=1, A271=1, A272=1, A273=1, A274=1, A275=1, A276=1, A277=1, A278=1, A279=1, A280=1, A281=1, A282=1, A283=1, A284=1, A285=1, A286=1, A287=1, A288=1, A289=1, A290=1, A291=1, A292=1, A293=1, A294=1, A295=1, A296=1, A297=1, A298=1, A299=1, A300=1, A301=1, A302=1, A303=1, A304=1, A305=1, A306=1, A307=1, A308=1, A309=1, A310=1, A311=1, A312=1, A313=1, A314=1, A315=1, A316=1, A317=1, A318=1, A319=1, A320=1, A321=1, A322=1, A323=1, A324=1, A325=1, A326=1, A327=1, A328=1, A329=1, A330=1, A331=1, A332=1, A333=1, A334=1, A335=1, A336=1, A337=1, A338=1, A339=1, A340=1, A341=1, A342=1, A343=1, A344=1, A345=1, A346=1, A347=1, A348=1, A349=1, A350=1, A351=1, A352=1, A353=1, A354=1, A355=1, A356=1, A357=1, A358=1, A359=1, A360=1, A361=1, A362=1, A363=1, A364=1, A365=1, A366=1, A367=1, A368=1, A369=1, A370=1, A371=1, A372=1, A373=1, A374=1, A375=1, A376=1, A377=1, A378=1, A379=1, A380=1, A381=1, A382=1, A383=1, A384=1, A385=1, A386=1, A387=1, A388=1, A389=1, A390=1, A391=1, A392=1, A393=1, A394=1, A395=1, A396=1, " +
                "A397=1, A398=1, A399=1}", throttledPublisher.getPublishCounts().toString(),"Verified that each symbol getting published only once");
        Thread.sleep(1000);
        throttledPublisher.publishData();
        assertEquals(400, throttledPublisher.getPublishCounts().keySet().size());

        marketDataGenerator.generateMockMarketData().forEach(marketDataProcessor::onMessage);
        Thread.sleep(1000);
        throttledPublisher.publishData();
        assertEquals("{A0=2, A1=2, A2=2, A3=2, A4=2, A5=2, A6=2, A7=2, A8=2, A9=2, A10=2, A11=2, A12=2, A13=2, A14=2, A15=2, A16=2, A17=2, A18=2, A19=2, A20=2, A21=2, A22=2, A23=2, A24=2, A25=2, A26=2, A27=2, A28=2, A29=2, A30=2, A31=2, A32=2, A33=2, A34=2, A35=2, A36=2, A37=2, A38=2, A39=2, A40=2, A41=2, A42=2, A43=2, A44=2, A45=2, A46=2, A47=2, A48=2, A49=2, A50=2, A51=2, A52=2, A53=2, A54=2, A55=2, A56=2, A57=2, A58=2, A59=2, A60=2, A61=2, A62=2, A63=2, A64=2, A65=2, A66=2, A67=2, A68=2, A69=2, A70=2, A71=2, A72=2, A73=2, A74=2, A75=2, A76=2, A77=2, A78=2, A79=2, A80=2, A81=2, A82=2, A83=2, A84=2, A85=2, A86=2, A87=2, A88=2, A89=2, A90=2, A91=2, A92=2, A93=2, A94=2, A95=2, A96=2, A97=2, A98=2, A99=2, A100=1, A101=1, A102=1, A103=1, A104=1, A105=1, A106=1, A107=1, A108=1, A109=1, A110=1, A111=1, A112=1, A113=1, A114=1, A115=1, A116=1, A117=1, A118=1, A119=1, A120=1, A121=1, A122=1, A123=1, A124=1, A125=1, A126=1, A127=1, A128=1, A129=1, A130=1, A131=1, A132=1, A133=1, A134=1, A135=1, A136=1, A137=1, A138=1, A139=1, A140=1, A141=1, A142=1, A143=1, A144=1, A145=1, A146=1, A147=1, A148=1, A149=1, A150=1, A151=1, A152=1, A153=1, A154=1, A155=1, A156=1, A157=1, A158=1, A159=1, A160=1, A161=1, A162=1, A163=1, A164=1, A165=1, A166=1, A167=1, A168=1, A169=1, A170=1, A171=1, A172=1, A173=1, A174=1, A175=1, A176=1, A177=1, A178=1, A179=1, A180=1, A181=1, A182=1, A183=1, A184=1, A185=1, A186=1, A187=1, A188=1, A189=1, A190=1, A191=1, A192=1, A193=1, A194=1, A195=1, A196=1, A197=1, A198=1, A199=1, A200=1, A201=1, A202=1, A203=1, A204=1, A205=1, A206=1, A207=1, A208=1, A209=1, A210=1, A211=1, A212=1, A213=1, A214=1, A215=1, A216=1, A217=1, A218=1, A219=1, A220=1, A221=1, A222=1, A223=1, A224=1, A225=1, A226=1, A227=1, A228=1, A229=1, A230=1, A231=1, A232=1, A233=1, A234=1, A235=1, A236=1, A237=1, A238=1, A239=1, A240=1, A241=1, A242=1, A243=1, A244=1, A245=1, A246=1, A247=1, A248=1, A249=1, A250=1, A251=1, A252=1, A253=1, A254=1, A255=1, A256=1, A257=1, A258=1, A259=1, A260=1, A261=1, A262=1, A263=1, A264=1, A265=1, A266=1, A267=1, A268=1, A269=1, A270=1, A271=1, A272=1, A273=1, A274=1, A275=1, A276=1, A277=1, A278=1, A279=1, A280=1, A281=1, A282=1, A283=1, A284=1, A285=1, A286=1, A287=1, A288=1, A289=1, A290=1, A291=1, A292=1, A293=1, A294=1, A295=1, A296=1, A297=1, A298=1, A299=1, A300=1, A301=1, A302=1, A303=1, A304=1, A305=1, A306=1, A307=1, A308=1, A309=1, A310=1, A311=1, A312=1, A313=1, A314=1, A315=1, A316=1, A317=1, A318=1, A319=1, A320=1, A321=1, A322=1, A323=1, A324=1, A325=1, A326=1, A327=1, A328=1, A329=1, A330=1, A331=1, A332=1, A333=1, A334=1, A335=1, A336=1, A337=1, A338=1, A339=1, A340=1, A341=1, A342=1, A343=1, A344=1, A345=1, A346=1, A347=1, A348=1, A349=1, A350=1, A351=1, A352=1, A353=1, A354=1, A355=1, A356=1, A357=1, A358=1, A359=1, A360=1, A361=1, A362=1, A363=1, A364=1, A365=1, A366=1, A367=1, A368=1, A369=1, A370=1, A371=1, A372=1, A373=1, A374=1, A375=1, A376=1, A377=1, A378=1, A379=1, A380=1, A381=1, A382=1, A383=1, A384=1, A385=1, A386=1, A387=1, A388=1, A389=1, A390=1, A391=1, A392=1, A393=1, A394=1, " +
                "A395=1, A396=1, A397=1, A398=1, A399=1}",throttledPublisher.getPublishCounts().toString(),"As expected the first 100 symbols get published again and thus publish count increments");

    }


    @Test
    public void testNonNumericSymbolsPublish() {
        SlidingWindow slidingWindow = new SlidingWindow(3, 1); //i.e. 3 requests per second
        ConflatingQueue conflatingQueue = new ConflatingQueue(1000000);
        MarketDataProcessor marketDataProcessor = new MarketDataProcessor(new MarketDataGenerator(1,1), conflatingQueue);

        Arrays.asList(MarketData.builder().symbol("A").price(new Price(1,1,1)).updateTime(Instant.MIN).build(),MarketData.builder().symbol("B").price(new Price(1,1,1)).updateTime(Instant.MIN).build()).forEach(marketDataProcessor::onMessage);
        assertEquals(2, marketDataProcessor.getConflatingQueue().getNoOfSymbols(), "40 market data events created");

        ThrottledPublisher throttledPublisher = new ThrottledPublisher(conflatingQueue, marketDataProcessor, slidingWindow);
        throttledPublisher.publishData();

        assertEquals("{A=1, B=1}", throttledPublisher.getPublishCounts().toString(),"Verified that non numeric symbol also getting counted");
    }
}
