package net.unit8.falchion;

import org.junit.Test;

import java.io.IOException;

/**
 * Created by kawasima on 16/04/07.
 */
public class InternalTest {
    @Test
    public void test() throws Exception {
        Class<?> aClass = Class.forName("sun.misc.VM");
        System.out.println(aClass);
    }
}
