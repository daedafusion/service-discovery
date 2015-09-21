package com.daedafusion.discovery;

import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by mphilpot on 8/29/14.
 */
public class IntegrationDriver
{
    private static final Logger log = Logger.getLogger(IntegrationDriver.class);

    @Test
    @Ignore
    public void main() throws Exception
    {
        System.setProperty("etcdHost", "192.168.59.103");
        String serviceName = "ontology";

        ServiceInstance instance1 = Discovery.getInstance().getInstance(serviceName);
        ServiceInstance instance2 = Discovery.getInstance().getInstance(serviceName);

        log.info(instance1.buildUriSpec());
        log.info(instance2.buildUriSpec());
    }
}
