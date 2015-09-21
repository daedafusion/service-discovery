package com.daedafusion.client.server;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by mphilpot on 8/14/14.
 */
@Path("/")
public class PingService
{
    private static final Logger log = Logger.getLogger(PingService.class);

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String ping(String input)
    {
        log.info("REACHED HERE IN PING SERVICE");
        return Base64.encodeBase64String(input.getBytes());
    }
}
