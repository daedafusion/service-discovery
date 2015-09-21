package com.daedafusion.discovery;

import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by mphilpot on 8/14/14.
 */
public class Discovery implements Closeable
{

    private static final Logger log = Logger.getLogger(Discovery.class);

    private static Discovery ourInstance = new Discovery();

    public static Discovery getInstance()
    {
        return ourInstance;
    }

    private DiscoveryProvider provider;

    private Discovery()
    {
        provider = loadProvider(System.getProperty("discoveryProvider",
                "com.daedafusion.discovery.providers.EtcdProvider"));
    }

    private DiscoveryProvider loadProvider(String providerClass)
    {
        try
        {
            DiscoveryProvider dp = (DiscoveryProvider) Class.forName(providerClass).newInstance();

            dp.init();

            return dp;
        }
        catch (Exception e)
        {
            log.error("", e);
            return null;
        }
    }

    public ServiceInstance<?> getInstance(String serviceName) throws Exception
    {
        return provider.getInstance(serviceName);
    }

    public void registerService(ServiceInstance<?> serviceInstance) throws Exception
    {
        provider.registerService(serviceInstance);
    }

    public void unregisterService(ServiceInstance<?> serviceInstance) throws Exception
    {
        provider.unregisterService(serviceInstance);
    }

    public void updateService(ServiceInstance<?> serviceInstance) throws Exception
    {
        provider.updateService(serviceInstance);
    }

    @Override
    public void close() throws IOException
    {
        provider.close();
    }
}
