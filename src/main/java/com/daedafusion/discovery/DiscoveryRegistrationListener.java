package com.daedafusion.discovery;

import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceInstanceBuilder;
import org.apache.curator.x.discovery.UriSpec;
import org.apache.log4j.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Created by mphilpot on 8/14/14.
 */
public class DiscoveryRegistrationListener implements ServletContextListener
{
    private static final Logger log = Logger.getLogger(DiscoveryRegistrationListener.class);

    private final String serviceName;
    private final String address;
    private final int port;
    private final boolean ssl;

    private ServiceInstance<String> instance;


    public DiscoveryRegistrationListener(String serviceName, int port, boolean ssl)
    {
        this(serviceName, null, port, ssl);
    }

    public DiscoveryRegistrationListener(String serviceName, String address, int port, boolean ssl)
    {
        this.serviceName = serviceName;
        this.address = address;
        this.port = port;
        this.ssl = ssl;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        try
        {
            ServiceInstanceBuilder<String> builder = ServiceInstance.<String>builder()
                    .name(serviceName)
                    .uriSpec(new UriSpec("{scheme}://{address}:{port}"));

            if(ssl)
            {
                builder.sslPort(port);
            }
            else
            {
                builder.port(port);
            }

            if(address != null)
            {
                builder.address(address);
            }

            instance = builder.build();

            log.info(String.format("Service Registration :: %s -> %s", serviceName, instance.buildUriSpec()));

            Discovery.getInstance().registerService(instance);
        }
        catch (Exception e)
        {
            log.error("", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
        try
        {
            Discovery.getInstance().unregisterService(instance);

            Discovery.getInstance().close();
        }
        catch (Exception e)
        {
            log.error("", e);
        }
    }
}
