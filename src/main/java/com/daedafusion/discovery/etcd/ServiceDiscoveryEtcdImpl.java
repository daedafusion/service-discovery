package com.daedafusion.discovery.etcd;

import com.daedafusion.jetcd.EtcdClient;
import com.daedafusion.jetcd.EtcdClientException;
import com.daedafusion.jetcd.EtcdNode;
import com.daedafusion.jetcd.EtcdResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.codec.binary.Base64;
import org.apache.curator.utils.ThreadUtils;
import org.apache.curator.x.discovery.*;
import org.apache.curator.x.discovery.details.InstanceSerializer;
import org.apache.curator.x.discovery.strategies.RoundRobinStrategy;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by mphilpot on 8/13/14.
 */
public class ServiceDiscoveryEtcdImpl<T> implements ServiceDiscovery<T>
{
    private static final Logger log = Logger.getLogger(ServiceDiscoveryEtcdImpl.class);

    private final EtcdClient            client;
    private final String basePath;
    private final InstanceSerializer<T> serializer;
    private final Map<String, ServiceInstance<T>> services  = Maps.newConcurrentMap();
    private final Collection<ServiceCache<T>> caches    = Sets.newSetFromMap(Maps.<ServiceCache<T>, Boolean>newConcurrentMap());
    private final Collection<ServiceProvider<T>> providers = Sets.newSetFromMap(Maps.<ServiceProvider<T>, Boolean>newConcurrentMap());

    private final ScheduledExecutorService executor;
    private final Map<String, ScheduledFuture> futures = Maps.newConcurrentMap();

    public ServiceDiscoveryEtcdImpl(EtcdClient client, String basePath, InstanceSerializer<T> serializer, ServiceInstance<T> thisInstance)
    {
        this.client = client;
        this.basePath = basePath;
        this.serializer = serializer;

        executor = Executors.newSingleThreadScheduledExecutor();

        if (thisInstance != null)
        {
            services.put(thisInstance.getId(), thisInstance);
        }
    }

    @Override
    public void start() throws Exception
    {
        reRegisterServices();
    }

    @Override
    public void registerService(ServiceInstance<T> service) throws Exception
    {
        services.put(service.getId(), service);
        internalRegisterService(service);
    }

    @Override
    public void updateService(ServiceInstance<T> service) throws Exception
    {
        Preconditions.checkArgument(services.containsKey(service.getId()), "Service is not registered: " + service);

        byte[]          bytes = serializer.serialize(service);
        String path = String.format("%s/instance", pathForInstance(service.getName(), service.getId()));

        client.set(path, Base64.encodeBase64String(bytes));
    }

    @Override
    public void unregisterService(ServiceInstance<T> service) throws Exception
    {
        String path = pathForInstance(service.getName(), service.getId());

        services.remove(service.getId());
        futures.remove(service.getId()).cancel(true);

        client.deleteDirectoryRecursive(path);
    }

    @Override
    public ServiceCacheBuilder<T> serviceCacheBuilder()
    {
        return new ServiceCacheBuilderImpl<T>(this)
                .threadFactory(ThreadUtils.newThreadFactory("ServiceCache"));
    }

    @Override
    public Collection<String> queryForNames() throws Exception
    {
        List<String> names = new ArrayList<>();

        for(EtcdNode node : client.listDirectory(basePath))
        {
            names.add(node.getKey().replace(basePath+"/", ""));
        }

        return ImmutableList.copyOf(names);
    }

    @Override
    public Collection<ServiceInstance<T>> queryForInstances(String name) throws Exception
    {
        ImmutableList.Builder<ServiceInstance<T>> builder = ImmutableList.builder();

        for(EtcdNode node : client.listDirectory(pathForName(name)))
        {
            ServiceInstance<T> instance = queryForInstance(name, node.getKey().replace(String.format("%s/%s/", basePath, name), ""));
            if(instance != null)
            {
                builder.add(instance);
            }
        }

        return builder.build();
    }

    @Override
    public ServiceInstance<T> queryForInstance(String name, String id) throws Exception
    {
        EtcdResult result = client.get(String.format("%s/instance", pathForInstance(name, id)));

        String b64Instance = result.getNode().getValue();

        byte[] bytes = Base64.decodeBase64(b64Instance);

        return serializer.deserialize(bytes);
    }

    @Override
    public ServiceProviderBuilder<T> serviceProviderBuilder()
    {
        return new ServiceProviderBuilderImpl<T>(this)
                .providerStrategy(new RoundRobinStrategy<T>())
                .threadFactory(ThreadUtils.newThreadFactory("ServiceProvider"));
    }

    @Override
    public void close() throws IOException
    {
        // TODO when registration methods implemented, this must handle clean up
        client.close();
    }

    void    cacheOpened(ServiceCache<T> cache)
    {
        caches.add(cache);
    }

    void    cacheClosed(ServiceCache<T> cache)
    {
        caches.remove(cache);
    }

    void    providerOpened(ServiceProvider<T> provider)
    {
        providers.add(provider);
    }

    void    providerClosed(ServiceProvider<T> cache)
    {
        providers.remove(cache);
    }

    private void reRegisterServices() throws Exception
    {
        for ( ServiceInstance<T> service : services.values() )
        {
            internalRegisterService(service);
        }
    }

    @VisibleForTesting
    protected void     internalRegisterService(ServiceInstance<T> service) throws Exception
    {
        final byte[]    bytes = serializer.serialize(service);
        final String path = pathForInstance(service.getName(), service.getId());

        client.createDirectory(path, 30);
        client.set(String.format("%s/instance", path), Base64.encodeBase64String(bytes));

        ScheduledFuture future = executor.scheduleAtFixedRate(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    client.refreshDirectory(path, 30);
                }
                catch (EtcdClientException e)
                {
                    log.error("", e);
                    if(e.getStatusCode().equals(404))
                    {
                        try
                        {
                            client.createDirectory(path, 30);
                            client.set(String.format("%s/instance", path), Base64.encodeBase64String(bytes));
                        }
                        catch (EtcdClientException e1)
                        {
                            log.warn("", e1);
                        }
                    }
                }
            }
        }, 5, 10, TimeUnit.SECONDS);

        futures.put(service.getId(), future);
    }

    String pathForInstance(String name, String id)
    {
        return String.format("%s/%s/%s", basePath, name, id);
    }

    String pathForName(String name)
    {
        return String.format("%s/%s", basePath, name);
    }

    EtcdClient getClient()
    {
        return client;
    }

    String getBasePath()
    {
        return basePath;
    }

    InstanceSerializer<T> getSerializer()
    {
        return serializer;
    }
}
