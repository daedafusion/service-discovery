package com.daedafusion.client;

import com.daedafusion.client.server.PingServer;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by mphilpot on 8/14/14.
 */
public class ClientTest
{
    private static final Logger log = Logger.getLogger(ClientTest.class);

    @Test
    @Ignore
    public void main() throws IOException, InterruptedException
    {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        PingServer server = new PingServer();
        server.setUseSsl(true);

        executor.execute(server);

        // Allow server to warm up
        TimeUnit.SECONDS.sleep(10);

        log.info("Starting Client");

        PingClient client = new PingClient();

        String input = "This is a test of the emergency broadcast system";

        log.info("************ PING ************");

        String response = client.testPing(input);

        assertThat(response, is(Base64.encodeBase64String(input.getBytes())));

        executor.shutdown();
    }

    @Test
    @Ignore
    public void externalTest()
    {
        PingServer server = new PingServer();
        server.setUseSsl(true);

        server.run();
    }
}
