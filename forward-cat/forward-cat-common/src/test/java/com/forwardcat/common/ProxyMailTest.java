package com.forwardcat.common;

import org.junit.Test;

public class ProxyMailTest extends SerializationTest {

    @Test
    public void userSerializes() {
        ProxyMail proxy = new ProxyMail("real@mail.com", "ts1", "ts2", "en");
        proxy.activate();
        proxy.block();
        assertSerializes(proxy, ProxyMail.class);
    }
}
