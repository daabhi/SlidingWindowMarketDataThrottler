package pojo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

@Getter @Setter @ToString @EqualsAndHashCode
public class ConflatingQueue {

    private final Map<String, MarketData> marketDataSymbolMap;
    private final Queue<String>           symbols;
    private final Set<String>             pendingSymbolsForPublishing;

    public ConflatingQueue(int size) {
        marketDataSymbolMap         = new HashMap<>(size);
        symbols                     = new LinkedBlockingQueue<>(5000);
        pendingSymbolsForPublishing = new LinkedHashSet<>();
    }

    /**
     * Conflation queue puts the latest market data for every symbol in the map. It gets overwritten for every symbol.
     * The key is the conflation key which is used for conflating the entries as when the queue is called, the latest data for the key is fetched.
     * @param marketData
     */
    public void offer(MarketData marketData) {
        String symbol = marketData.getSymbol();
        if(!marketDataSymbolMap.containsKey(symbol)){
            symbols.add(symbol);
        }
        marketDataSymbolMap.put(symbol, marketData);
        pendingSymbolsForPublishing.add(symbol);
    }

    /**
     * Gets the latest market data for the symbol from the map
     */
    public MarketData take() {
        MarketData marketData = null;
        if(!symbols.isEmpty()) {
            String symbol = symbols.remove();
            if(symbol!=null){
                marketData = marketDataSymbolMap.remove(symbol);
            }
        }
        return marketData;
    }
    /**
     * Peeks the latest market data for the symbol from the map
     */
    public MarketData peek() {
        MarketData marketData = null;
        if(!symbols.isEmpty()) {
            String symbol = symbols.peek();
            if(symbol!=null){
                marketData = marketDataSymbolMap.get(symbol);
            }
        }
        return marketData;
    }

    public boolean isEmpty() {
        return symbols.isEmpty();
    }

    public int getNoOfSymbols(){
        return symbols.size();
    }

    public MarketData getMarketData(String symbol){
        //Ideally should make this private as only used in tests and then use Whitebox.invokeMethod for private method testing
        if (marketDataSymbolMap.containsKey(symbol)){
            return marketDataSymbolMap.get(symbol);
        }
        return null;
    }
}
