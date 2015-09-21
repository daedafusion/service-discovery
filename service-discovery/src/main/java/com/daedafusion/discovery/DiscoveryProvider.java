package com.daedafusion.discovery;

import org.apache.curator.x.discovery.ServiceInstance;

import java.io.Closeable;

/**
 * Created by mphilpot on 8/14/14.
 */
public interface DiscoveryProvider<T> extends Closeable
{
    ServiceInstance<T> getInstance(String serviceName) throws Exception;

    void registerService(ServiceInstance<T> serviceInstance) throws Exception;
    void unregisterService(ServiceInstance<T> serviceInstance) throws Exception;
    void updateService(ServiceInstance<T> serviceInstance) throws Exception;

    void init() throws Exception;
}
