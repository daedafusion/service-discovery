package com.daedafusion.discovery.etcd;

import com.google.common.collect.Lists;
import org.apache.curator.x.discovery.*;
import org.apache.curator.x.discovery.details.InstanceProvider;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadFactory;

/**
 * Created by mphilpot on 8/14/14.
 */
public class ServiceProviderImpl<T> implements ServiceProvider<T>
{
    private static final Logger log = Logger.getLogger(ServiceProviderImpl.class);

    private final ServiceCache<T>         cache;
    private final InstanceProvider<T>     instanceProvider;
    private final ServiceDiscoveryEtcdImpl<T>     discovery;
    private final ProviderStrategy<T>     providerStrategy;
    private final DownInstanceManager<T>  downInstanceManager;

    public ServiceProviderImpl(ServiceDiscoveryEtcdImpl<T> discovery, String serviceName,
                               ProviderStrategy<T> providerStrategy, ThreadFactory threadFactory,
                               List<InstanceFilter<T>> filters, DownInstancePolicy downInstancePolicy)
    {
        this.discovery = discovery;
        this.providerStrategy = providerStrategy;

        downInstanceManager = new DownInstanceManager<T>(downInstancePolicy);
        cache = discovery.serviceCacheBuilder().name(serviceName).threadFactory(threadFactory).build();

        List<InstanceFilter<T>> localFilters = Lists.newArrayList(filters);
        localFilters.add(downInstanceManager);
        instanceProvider = new FilteredInstanceProvider<T>(cache, localFilters);
    }

    @Override
    public void start() throws Exception
    {
        cache.start();
        discovery.providerOpened(this);
    }

    @Override
    public ServiceInstance<T> getInstance() throws Exception
    {
        return providerStrategy.getInstance(instanceProvider);
    }

    @Override
    public Collection<ServiceInstance<T>> getAllInstances() throws Exception
    {
        return instanceProvider.getInstances();
    }

    @Override
    public void noteError(ServiceInstance<T> instance)
    {
        downInstanceManager.add(instance);
    }

    @Override
    public void close() throws IOException
    {
        discovery.providerClosed(this);
        cache.close();
    }
}
