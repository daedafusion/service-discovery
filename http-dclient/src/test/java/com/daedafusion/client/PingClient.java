package com.daedafusion.client;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * Created by mphilpot on 8/14/14.
 */
public class PingClient extends AbstractClient
{
    private static final Logger log = Logger.getLogger(PingClient.class);

    protected PingClient()
    {
        super("ping");
    }

    public String testPing(String input) throws IOException
    {
        HttpPost post = new HttpPost(baseUrl);

        post.setEntity(new StringEntity(input, ContentType.TEXT_PLAIN));

        try (CloseableHttpResponse response = client.execute(post))
        {
            StatusLine line = response.getStatusLine();

            if (line.getStatusCode() >= 300)
            {
                throw new IOException(line.getReasonPhrase());
            }

            return EntityUtils.toString(response.getEntity());
        }
    }
}
