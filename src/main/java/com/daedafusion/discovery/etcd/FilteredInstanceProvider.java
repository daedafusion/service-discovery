package com.daedafusion.discovery.etcd;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.curator.x.discovery.InstanceFilter;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.InstanceProvider;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by mphilpot on 8/14/14.
 */
public class FilteredInstanceProvider<T> implements InstanceProvider<T>
{
    private static final Logger log = Logger.getLogger(FilteredInstanceProvider.class);

    private final InstanceProvider<T>           instanceProvider;
    private final Predicate<ServiceInstance<T>> predicates;

    FilteredInstanceProvider(InstanceProvider<T> instanceProvider, List<InstanceFilter<T>> filters)
    {
        this.instanceProvider = instanceProvider;
        predicates = Predicates.and(filters);
    }

    @Override
    public List<ServiceInstance<T>> getInstances() throws Exception
    {
        Iterable<ServiceInstance<T>> filtered = Iterables.filter(instanceProvider.getInstances(), predicates);
        return ImmutableList.copyOf(filtered);
    }
}
