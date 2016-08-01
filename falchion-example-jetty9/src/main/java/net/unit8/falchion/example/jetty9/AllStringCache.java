package net.unit8.falchion.example.jetty9;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.UUID;

/**
 * @author kawasima
 */
public class AllStringCache {
    private Cache<String, String> cache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .build();

    public void put(String value) {
        cache.put(UUID.nameUUIDFromBytes(value.getBytes()).toString(), value);
    }
}
