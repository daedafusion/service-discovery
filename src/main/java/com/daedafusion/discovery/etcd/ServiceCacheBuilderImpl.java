package com.daedafusion.discovery.etcd;

import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceCacheBuilder;
import org.apache.log4j.Logger;

import java.util.concurrent.ThreadFactory;

/**
 * Created by mphilpot on 8/14/14.
 */
public class ServiceCacheBuilderImpl<T> implements ServiceCacheBuilder<T>
{
    private static final Logger log = Logger.getLogger(ServiceCacheBuilderImpl.class);

    private ServiceDiscoveryEtcdImpl<T> discovery;
    private String name;
    private ThreadFactory threadFactory;

    ServiceCacheBuilderImpl(ServiceDiscoveryEtcdImpl<T> discovery)
    {
        this.discovery = discovery;
    }

    /**
     * Return a new service cache with the current settings
     *
     * @return service cache
     */
    @Override
    public ServiceCache<T> build()
    {
        return new ServiceCacheImpl<T>(discovery, name, threadFactory);
    }

    /**
     * The name of the service to cache (required)
     *
     * @param name service name
     * @return this
     */
    @Override
    public ServiceCacheBuilder<T> name(String name)
    {
        this.name = name;
        return this;
    }

    /**
     * Optional thread factory to use for the cache's internal thread
     *
     * @param threadFactory factory
     * @return this
     */
    @Override
    public ServiceCacheBuilder<T> threadFactory(ThreadFactory threadFactory)
    {
        this.threadFactory = threadFactory;
        return this;
    }
}
