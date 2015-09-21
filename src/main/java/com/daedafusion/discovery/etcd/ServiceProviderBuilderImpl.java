package com.daedafusion.discovery.etcd;

import com.google.common.collect.Lists;
import org.apache.curator.x.discovery.*;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.ThreadFactory;

/**
 * Created by mphilpot on 8/13/14.
 */
public class ServiceProviderBuilderImpl<T> implements ServiceProviderBuilder<T>
{
    private static final Logger log = Logger.getLogger(ServiceProviderBuilderImpl.class);

    private ServiceDiscoveryEtcdImpl<T> discovery;
    private String serviceName;
    private ProviderStrategy<T> providerStrategy;
    private ThreadFactory threadFactory;
    private List<InstanceFilter<T>> filters            = Lists.newArrayList();
    private DownInstancePolicy      downInstancePolicy = new DownInstancePolicy();

    public ServiceProvider<T> build()
    {
        return new ServiceProviderImpl<T>(discovery, serviceName, providerStrategy, threadFactory, filters, downInstancePolicy);
    }

    @Override
    public ServiceProviderBuilder<T> serviceName(String serviceName)
    {
        this.serviceName = serviceName;
        return this;
    }

    @Override
    public ServiceProviderBuilder<T> providerStrategy(ProviderStrategy<T> providerStrategy)
    {
        this.providerStrategy = providerStrategy;
        return this;
    }

    @Override
    public ServiceProviderBuilder<T> threadFactory(ThreadFactory threadFactory)
    {
        this.threadFactory = threadFactory;
        return this;
    }

    @Override
    public ServiceProviderBuilder<T> downInstancePolicy(DownInstancePolicy downInstancePolicy)
    {
        this.downInstancePolicy = downInstancePolicy;
        return this;
    }

    @Override
    public ServiceProviderBuilder<T> additionalFilter(InstanceFilter<T> filter)
    {
        filters.add(filter);
        return this;
    }

    protected ServiceProviderBuilderImpl(ServiceDiscoveryEtcdImpl<T> discovery)
    {
        this.discovery = discovery;
    }
}
