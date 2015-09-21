package com.daedafusion.discovery.etcd;

import com.google.common.collect.Maps;
import org.apache.curator.x.discovery.DownInstancePolicy;
import org.apache.curator.x.discovery.InstanceFilter;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.log4j.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by mphilpot on 8/14/14.
 */
public class DownInstanceManager<T> implements InstanceFilter<T>
{
    private static final Logger log = Logger.getLogger(DownInstanceManager.class);

    private final ConcurrentMap<ServiceInstance<?>, Status> statuses = Maps.newConcurrentMap();
    private final DownInstancePolicy downInstancePolicy;
    private final AtomicLong lastPurge = new AtomicLong(System.currentTimeMillis());

    private static class Status
    {
        private final long          startMs    = System.currentTimeMillis();
        private final AtomicInteger errorCount = new AtomicInteger(0);
    }

    DownInstanceManager(DownInstancePolicy downInstancePolicy)
    {
        this.downInstancePolicy = downInstancePolicy;
    }

    void add(ServiceInstance<?> instance)
    {
        purge();

        Status newStatus = new Status();
        Status oldStatus = statuses.putIfAbsent(instance, newStatus);
        Status useStatus = (oldStatus != null) ? oldStatus : newStatus;
        useStatus.errorCount.incrementAndGet();
    }

    @Override
    public boolean apply(ServiceInstance<T> instance)
    {
        purge();

        Status status = statuses.get(instance);
        return (status == null) || (status.errorCount.get() < downInstancePolicy.getErrorThreshold());
    }

    private void purge()
    {
        long localLastPurge = lastPurge.get();
        long ticksSinceLastPurge = System.currentTimeMillis() - localLastPurge;
        if (ticksSinceLastPurge < (downInstancePolicy.getTimeoutMs() / 2) )
        {
            return;
        }

        if ( !lastPurge.compareAndSet(localLastPurge, System.currentTimeMillis()) )
        {
            return;
        }

        Iterator<Map.Entry<ServiceInstance<?>, Status>> it = statuses.entrySet().iterator();
        while ( it.hasNext() )
        {
            Map.Entry<ServiceInstance<?>, Status> entry = it.next();
            long elapsedMs = System.currentTimeMillis() - entry.getValue().startMs;
            if ( elapsedMs >= downInstancePolicy.getTimeoutMs() )
            {
                it.remove();
            }
        }
    }
}
