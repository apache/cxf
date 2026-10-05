package org.apache.cxf.ext.logging;

import junit.framework.TestCase;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class LoggingOutputStreamTest {

    private static final String APPLICATION_XML = "application/xml";
    private LogEventSenderMock logEventSender = new LogEventSenderMock();

    @Test
    public void shouldNotLogTwice() throws IOException {
        // Arrange
        final LoggingOutInterceptor outInterceptor = new LoggingOutInterceptor(logEventSender);

        final Message message = prepareOutMessage();

        String loggingContent = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:v1="http://ws.schema/example/v1"
                                  xmlns:xop="http://w3.org">
                    <soapenv:Body>
                        <v1:plutoResponse>
                            <v1:responseNumber>1</v1:responseNumber>
                            <v1:allegatoFile>dddddddddddddd</v1:allegatoFile>
                        </v1:plutoResponse>
                    </soapenv:Body>
                </soapenv:Envelope>
                """;


        // Act
        outInterceptor.handleMessage(message);
        byte[] payload = loggingContent.getBytes(StandardCharsets.UTF_8);
        OutputStream out = message.getContent(OutputStream.class);
        out.write(payload);
        out.close();

        // Verify
        LogEvent event = logEventSender.getLogEvent();
        assertNotNull(event);
        assertEquals(event.getPayload(), loggingContent);

        // Should not log twice
        logEventSender.getLogEvents().clear();
        out.close();
        assertEquals(0 , logEventSender.getLogEvents().size());
    }

    @Test
    public void shouldLogEvenIfIOException() throws IOException {
        // Arrange
        final LoggingOutInterceptor outInterceptor = new LoggingOutInterceptor(logEventSender);

        final Message message = prepareOutMessage();

        // Counter to let some bytes to be written onto the http socket before thrown IOException
        final AtomicInteger written = new AtomicInteger(0);
        final int limitIO = 244;
        OutputStream mockUnderlyingStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                if (written.incrementAndGet() > limitIO) {
                    throw new IOException("Connection reset by peer");
                }
            }
        };
        message.setContent(OutputStream.class, mockUnderlyingStream);

        String loggingContent = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:v1="http://ws.schema/example/v1"
                                  xmlns:xop="http://w3.org">
                    <soapenv:Body>
                        <v1:plutoResponse>
                            <v1:responseNumber>1</v1:responseNumber>
                            <v1:allegatoFile>dddddddddddddd</v1:allegatoFile>
                        </v1:plutoResponse>
                    </soapenv:Body>
                </soapenv:Envelope>
                """;

        // Act
        outInterceptor.handleMessage(message);

        byte[] payload = loggingContent.getBytes(StandardCharsets.UTF_8);
        byte[] firstChunk = new byte[loggingContent.length()/2];
        byte[] secondChunk = new byte[loggingContent.length()-firstChunk.length];

        System.arraycopy(payload, 0, firstChunk, 0, firstChunk.length);
        System.arraycopy(payload, 200, secondChunk, 0, secondChunk.length);

        OutputStream out = message.getContent(OutputStream.class);
        try{
            // First chunk of bytes OK (mock limit is above ...244)
            out.write(firstChunk);
            assertTrue(true);
            // Second chunk of byte the peer reset :(
            out.write(secondChunk);
            fail();
            out.close();
        } catch (IOException ex){
            //Ensure the exception is propagated
            assertTrue(true);
        }

        // Verify
        LogEvent event = logEventSender.getLogEvent();
        assertNotNull(event);
        // Assert the partial log (only what was wrote onto the http socket)
        assertEquals(event.getPayload(), loggingContent.substring(0, loggingContent.length()/2));

        // Should not log twice
        logEventSender.getLogEvents().clear();
        out.close();
        assertEquals(0 , logEventSender.getLogEvents().size());
    }

    private Message prepareOutMessage() {
        Message message = new MessageImpl();
        message.put(Message.CONTENT_TYPE, APPLICATION_XML);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.setContent(OutputStream.class, outputStream);
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        return message;
    }
}