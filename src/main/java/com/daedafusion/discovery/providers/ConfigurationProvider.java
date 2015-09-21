package com.daedafusion.discovery.providers;

import com.daedafusion.configuration.Configuration;
import com.daedafusion.discovery.DiscoveryProvider;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.UriSpec;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * Created by mphilpot on 8/14/14.
 */
public class ConfigurationProvider implements DiscoveryProvider<String>
{
    private static final Logger log = Logger.getLogger(ConfigurationProvider.class);

    @Override
    public ServiceInstance<String> getInstance(String serviceName) throws Exception
    {
        String url = Configuration.getInstance().getString(serviceName);

        if(url == null)
        {
            return null;
        }

        return ServiceInstance.<String>builder()
                .name(serviceName)
                .payload("")
                .address(url)
                .uriSpec(new UriSpec("{address}"))
                .build();
    }

    @Override
    public void registerService(ServiceInstance<String> serviceInstance)
    {
        // Empty
    }

    @Override
    public void unregisterService(ServiceInstance<String> serviceInstance)
    {
        // Empty
    }

    @Override
    public void updateService(ServiceInstance<String> serviceInstance)
    {
        // Empty
    }

    @Override
    public void init()
    {
        // Empty
    }

    @Override
    public void close() throws IOException
    {
        // Empty
    }
}
