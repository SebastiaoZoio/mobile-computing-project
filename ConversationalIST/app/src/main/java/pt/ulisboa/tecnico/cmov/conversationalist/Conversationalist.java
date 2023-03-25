package pt.ulisboa.tecnico.cmov.conversationalist;

import android.app.Application;
import android.util.LruCache;
import java.util.List;
import java.util.Map;

public class Conversationalist extends Application {
    private LruCache<String, List<BasicMessage>> mMemoryCache;
    private LruCache<String, List<Map<String, String>>> mChatsCache;

    @Override
    public void onCreate(){
        super.onCreate();
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, List<BasicMessage>>(cacheSize);
        mChatsCache = new LruCache<String, List<Map<String, String>>>(cacheSize);
    }

    public List<BasicMessage> getMessagesFromCache(String chatName){
        return this.mMemoryCache.get(chatName);
    }

    public void addMessagesToCache(String chatName, List<BasicMessage> messages){
        if (this.getMessagesFromCache(chatName) != null) {
            mMemoryCache.remove(chatName);
        }
        mMemoryCache.put(chatName, messages);
    }

    public LruCache<String, List<BasicMessage>> getMemoryCache(){
        return this.mMemoryCache;
    }

    public List<Map<String, String>> getChatsFromCache(String username){
        return this.mChatsCache.get(username);
    }

    public void addChatsToCache(String username, List<Map<String, String>> chats){
        if (this.getChatsFromCache(username) != null) {
            mChatsCache.remove(username);
        }
        mChatsCache.put(username, chats);
    }

    public LruCache<String, List<Map<String, String>>> getChatsCache(){
        return this.mChatsCache;
    }
}
