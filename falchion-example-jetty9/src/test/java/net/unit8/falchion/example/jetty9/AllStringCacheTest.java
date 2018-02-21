package net.unit8.falchion.example.jetty9;

import org.junit.Test;

import java.util.Random;
import java.util.stream.Stream;

/**
 * @author kawasima
 */
public class AllStringCacheTest {
    private static final String CHARS = "0123456789ABCDEF";
    final Random rand = new Random();

    private String randomString() {
        return rand.ints(0, CHARS.length())
                .mapToObj(CHARS::charAt)
                .limit(4096)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    @Test
    public void test() {
        AllStringCache cache = new AllStringCache();
        Stream.generate(this::randomString)
                .limit(3)
                .forEach(cache::put);
    }
}
