package com.daedafusion.client.server;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.apache.log4j.Logger;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Created by mphilpot on 8/14/14.
 */
public class Application extends ResourceConfig
{
    private static final Logger log = Logger.getLogger(Application.class);

    public Application()
    {
        super(
                PingService.class,
                JacksonJsonProvider.class
        );
    }
}
