package com.daedafusion.discovery.etcd;

import com.daedafusion.jetcd.EtcdClient;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.InstanceSerializer;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.log4j.Logger;

/**
 * Created by mphilpot on 8/13/14.
 */
public class ServiceDiscoveryEtcdBuilder<T>
{
    private static final Logger log = Logger.getLogger(ServiceDiscoveryEtcdBuilder.class);

    private EtcdClient            client;
    private String basePath;
    private InstanceSerializer<T> serializer;
    private ServiceInstance<T>    thisInstance;
    private Class<T> payloadClass;

    public static <T> ServiceDiscoveryEtcdBuilder<T> builder(Class<T> payloadClass)
    {
        return new ServiceDiscoveryEtcdBuilder<T>(payloadClass);
    }

    public ServiceDiscovery<T> build()
    {
        if (serializer == null)
        {
            serializer(new JsonInstanceSerializer<T>(payloadClass));
        }
        return new ServiceDiscoveryEtcdImpl<T>(client, basePath, serializer, thisInstance);
    }

    public ServiceDiscoveryEtcdBuilder<T> client(EtcdClient client)
    {
        this.client = client;
        return this;
    }

    public ServiceDiscoveryEtcdBuilder<T> basePath(String basePath)
    {
        this.basePath = basePath;
        return this;
    }

    public ServiceDiscoveryEtcdBuilder<T> serializer(InstanceSerializer<T> serializer)
    {
        this.serializer = serializer;
        return this;
    }

    /**
     * Optional
     *
     * @param thisInstance
     * @return
     */
    public ServiceDiscoveryEtcdBuilder<T> thisInstance(ServiceInstance<T> thisInstance)
    {
        this.thisInstance = thisInstance;
        return this;
    }

    private ServiceDiscoveryEtcdBuilder(Class<T> payloadClass)
    {
        this.payloadClass = payloadClass;
    }
}
