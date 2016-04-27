package net.unit8.falchion;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author kawasima
 */
public class TestMain {
    public static void main(String... args) {
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.execute(() -> {
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                service.shutdown();
            }
        });
    }
}
