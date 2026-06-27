package com.feilonglab.smtp.basic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SmtpClientTest {

    private Object getFieldValue(Object obj, String fieldName) throws Exception {
        java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    @Test
    public void testDefaultConstructor() throws Exception {
        // By default, it loads "/mail.properties".
        // In the test classpath, this resolves to src/test/resources/mail.properties
        SmtpClient client = new SmtpClient();
        assertEquals("smtp.test.com", getFieldValue(client, "host"));
        assertEquals(465, getFieldValue(client, "port"));
        assertEquals("testuser", getFieldValue(client, "username"));
        assertEquals("testpass", getFieldValue(client, "password"));
        assertEquals(false, getFieldValue(client, "useTls"));
        assertEquals(true, getFieldValue(client, "useSsl"));
        assertEquals("3000", getFieldValue(client, "connectionTimeoutMs"));
        assertEquals("4000", getFieldValue(client, "timeoutMs"));
        assertEquals("2000", getFieldValue(client, "writeTimeoutMs"));
    }

    @Test
    public void testCustomConstructor() throws Exception {
        SmtpClient client = new SmtpClient("custom.host.com", 25, "cuser", "cpass", false, true);
        assertEquals("custom.host.com", getFieldValue(client, "host"));
        assertEquals(25, getFieldValue(client, "port"));
        assertEquals("cuser", getFieldValue(client, "username"));
        assertEquals("cpass", getFieldValue(client, "password"));
        assertEquals(false, getFieldValue(client, "useTls"));
        assertEquals(true, getFieldValue(client, "useSsl"));
        // Default timeouts should be 5000
        assertEquals("5000", getFieldValue(client, "connectionTimeoutMs"));
        assertEquals("5000", getFieldValue(client, "timeoutMs"));
        assertEquals("5000", getFieldValue(client, "writeTimeoutMs"));


    }

    @Test
    public void testSendMailThrowsWhenNotOpened() {
        SmtpClient client = new SmtpClient(false);
        assertThrows(IllegalStateException.class, () -> client.sendMail("Sender", "recipient@example.com", "Subject", "Content"));
    }

    @Test
    public void testCloseDoesNotThrow() {
        SmtpClient client = new SmtpClient(false);
        assertDoesNotThrow(client::close);
    }
}


