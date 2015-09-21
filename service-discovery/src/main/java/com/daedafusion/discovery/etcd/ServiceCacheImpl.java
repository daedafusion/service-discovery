package com.daedafusion.discovery.etcd;

import com.daedafusion.jetcd.EtcdNode;
import com.daedafusion.jetcd.EtcdResult;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.codec.binary.Base64;
import org.apache.curator.framework.listen.ListenerContainer;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.ServiceCacheListener;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by mphilpot on 8/14/14.
 */
public class ServiceCacheImpl<T> implements ServiceCache<T>
{
    private static final Logger log = Logger.getLogger(ServiceCacheImpl.class);

    private final ListenerContainer<ServiceCacheListener> listenerContainer = new ListenerContainer<ServiceCacheListener>();
    private final ServiceDiscoveryEtcdImpl<T> discovery;
    private final AtomicReference<State> state     = new AtomicReference<State>(State.LATENT);
    private final Map<String, ServiceInstance<T>> instances = Maps.newConcurrentMap();
    private final String name;

    private final ExecutorService executor;

    private enum State
    {
        LATENT,
        STARTED,
        STOPPED
    }

    public ServiceCacheImpl(ServiceDiscoveryEtcdImpl<T> discovery, String name, ThreadFactory threadFactory)
    {
        Preconditions.checkNotNull(discovery, "discovery cannot be null");
        Preconditions.checkNotNull(name, "name cannot be null");
        Preconditions.checkNotNull(threadFactory, "threadFactory cannot be null");

        this.discovery = discovery;
        executor = Executors.newSingleThreadScheduledExecutor();
        this.name = name;
    }

    @Override
    public List<ServiceInstance<T>> getInstances()
    {
        return Lists.newArrayList(instances.values());
    }

    @Override
    public void start() throws Exception
    {
        Preconditions.checkState(state.compareAndSet(State.LATENT, State.STARTED), "Cannot be started more than once");

        for(EtcdNode instanceNode : discovery.getClient().listDirectory(discovery.pathForName(name)))
        {
            String instancePath = instanceNode.getKey();

            EtcdResult result = discovery.getClient().get(String.format("%s/instance", instancePath));

            ServiceInstance<T> serviceInstance = discovery.getSerializer().deserialize(Base64.decodeBase64(result.getNode().getValue()));

            instances.put(serviceInstance.getId(), serviceInstance);
        }

        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    ListenableFuture<EtcdResult> future = discovery.getClient().watch(discovery.pathForName(name), true);

                    // Don't care about result -- Will compute change below
                    future.get();

                    log.debug(String.format("SD:change for %s", discovery.pathForName(name)));

                    instances.clear();

                    try
                    {
                        for (EtcdNode instanceNode : discovery.getClient().listDirectory(discovery.pathForName(name)))
                        {
                            String instancePath = instanceNode.getKey();

                            EtcdResult er = discovery.getClient().get(String.format("%s/instance", instancePath));

                            ServiceInstance<T> serviceInstance = discovery.getSerializer().deserialize(Base64.decodeBase64(er.getNode().getValue()));

                            instances.put(serviceInstance.getId(), serviceInstance);
                        }
                    }
                    catch(Exception e)
                    {
                        log.error(e.getMessage());
                    }

                    listenerContainer.forEach(new Function<ServiceCacheListener, Void>()
                    {
                        @Override
                        public Void apply(ServiceCacheListener listener)
                        {
                            listener.cacheChanged();
                            return null;
                        }
                    });

                    executor.execute(this);
                }
                catch (Exception e)
                {
                    log.error("", e);

                }
            }
        });

        discovery.cacheOpened(this);
    }

    @Override
    public void close() throws IOException
    {
        Preconditions.checkState(state.compareAndSet(State.STARTED, State.STOPPED), "Already closed or has not been started");

        executor.shutdown();
        discovery.cacheClosed(this);
    }

    @Override
    public void addListener(ServiceCacheListener listener)
    {
        listenerContainer.addListener(listener);
    }

    @Override
    public void addListener(ServiceCacheListener listener, Executor executor)
    {
        listenerContainer.addListener(listener, executor);
    }

    @Override
    public void removeListener(ServiceCacheListener listener)
    {
        listenerContainer.removeListener(listener);
    }
}
