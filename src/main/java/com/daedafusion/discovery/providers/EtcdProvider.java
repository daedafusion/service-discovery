package com.daedafusion.discovery.providers;

import com.daedafusion.configuration.Configuration;
import com.daedafusion.discovery.DiscoveryProvider;
import com.daedafusion.discovery.etcd.ServiceDiscoveryEtcdBuilder;
import com.daedafusion.jetcd.EtcdClient;
import com.daedafusion.jetcd.EtcdClientFactory;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;
import org.apache.curator.x.discovery.strategies.RoundRobinStrategy;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mphilpot on 8/14/14.
 */
public class EtcdProvider implements DiscoveryProvider<String>
{
    private static final Logger log = Logger.getLogger(EtcdProvider.class);

    private ServiceDiscovery<String> discovery;

    private Map<String, ServiceProvider<String>> providers;

    @Override
    public ServiceInstance<String> getInstance(String serviceName) throws Exception
    {
        if(!providers.containsKey(serviceName))
        {
            ServiceProvider<String> p = discovery.serviceProviderBuilder()
                    .serviceName(serviceName)
                    .providerStrategy(new RoundRobinStrategy<String>())
                    .build();
            p.start();

            providers.put(serviceName, p);
        }

        return providers.get(serviceName).getInstance();
    }

    @Override
    public void registerService(ServiceInstance<String> serviceInstance) throws Exception
    {
        discovery.registerService(serviceInstance);
    }

    @Override
    public void unregisterService(ServiceInstance<String> serviceInstance) throws Exception
    {
        discovery.unregisterService(serviceInstance);
    }

    @Override
    public void updateService(ServiceInstance<String> serviceInstance) throws Exception
    {
        discovery.updateService(serviceInstance);
    }

    @Override
    public void init() throws Exception
    {
        EtcdClient client = EtcdClientFactory.newInstance();

        String basePath = Configuration.getInstance().getString("serviceDiscovery.basePath", "/discovery");

        discovery = ServiceDiscoveryEtcdBuilder.builder(String.class)
                .client(client)
                .basePath(basePath)
                .build();

        discovery.start();

        providers = new HashMap<>();
    }

    @Override
    public void close() throws IOException
    {
        for(ServiceProvider<String> provider : providers.values())
        {
            provider.close();
        }

        discovery.close();
    }
}
