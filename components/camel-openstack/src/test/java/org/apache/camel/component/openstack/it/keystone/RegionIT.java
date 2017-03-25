package org.apache.camel.component.openstack.it.keystone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.openstack.it.AbstractITSupport;
import org.apache.camel.component.openstack.keystone.KeystoneConstants;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.hamcrest.Matchers;
import org.openstack4j.api.Builders;
import org.openstack4j.model.identity.v3.Region;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

@Ignore("https://github.com/ContainX/openstack4j/issues/902")
public class RegionIT extends AbstractITSupport {

    @Test
    public void create() throws IOException, InterruptedException {
        Map<String, Object> headers = new HashMap();
        headers.put(KeystoneConstants.OPERATION, KeystoneConstants.CREATE);
        final String desc = "desc" + System.currentTimeMillis();
        headers.put(KeystoneConstants.DESCRIPTION, desc);

        Region created = template.requestBodyAndHeaders("direct:start", null, headers, Region.class);

        Assert.assertEquals(desc, created.getDescription());
        assertNotNull(created.getId());

        await().until(new ListCallable(), Matchers.hasItem(
                Matchers.allOf(
                        Matchers.hasProperty("id", Matchers.equalTo(created.getId())),
                        Matchers.hasProperty("description", Matchers.equalTo(desc))
                )));
    }

    @Test
    public void get() {
        final Region created = osclient.identity().regions().create(Builders.region().description("desc" + System.currentTimeMillis()).build());

        await().until(new ListCallable(), Matchers.hasItem(
                Matchers.hasProperty("id", Matchers.equalTo(created.getId()))
        ));

        Map<String, Object> headers = new HashMap();
        headers.put(KeystoneConstants.OPERATION, KeystoneConstants.GET);
        headers.put(KeystoneConstants.ID, created.getId());

        Region k = template.requestBodyAndHeaders("direct:start", null, headers, Region.class);
        Assert.assertEquals(k.getId(), created.getId());
        Assert.assertEquals(k.getDescription(), created.getDescription());
    }

    @Test
    public void getAll() {
        Region result = osclient.identity().regions().create(Builders.region().description("desc").build());

        await().until(new ListCallable(), Matchers.hasItem(
                Matchers.hasProperty("id", Matchers.equalTo(result.getId()))
        ));

        Map<String, Object> headers = new HashMap();
        headers.put(KeystoneConstants.OPERATION, KeystoneConstants.GET_ALL);
        List<Region> k = template.requestBodyAndHeaders("direct:start", null, headers, List.class);

        assertThat(k, Matchers.hasItem(Matchers.hasProperty("id", Matchers.equalTo(result.getId()))));
    }

    @Test
    public void update() {
        final String description = "description " + System.currentTimeMillis();
        final String newDescription = UUID.randomUUID().toString();
        Region created = osclient.identity().regions().create(Builders.region().description(description).build());

        await().until(new ListCallable(), Matchers.hasItem(
                Matchers.allOf(
                        Matchers.hasProperty("id", Matchers.equalTo(created)),
                        Matchers.hasProperty("description", Matchers.equalTo(description))
                )));

        Map<String, Object> headers = new HashMap();
        headers.put(KeystoneConstants.OPERATION, KeystoneConstants.UPDATE);

        template.requestBodyAndHeaders("direct:start", created.toBuilder().description(newDescription).build(), headers);

        await().until(new ListCallable(), Matchers.hasItem(
                Matchers.allOf(
                        Matchers.hasProperty("id", Matchers.equalTo(created.getId())),
                        Matchers.hasProperty("description", Matchers.equalTo(newDescription))
                )));
    }

    @Test
    public void delete() {
        Region created = osclient.identity().regions().create(Builders.region().description("desc").build());

        await().until(new ListCallable(), Matchers.hasItem(
                Matchers.hasProperty("id", Matchers.equalTo(created.getId()))
        ));

        Map<String, Object> headers = new HashMap();
        headers.put(KeystoneConstants.OPERATION, KeystoneConstants.DELETE);
        headers.put(KeystoneConstants.DOMAIN_ID, created.getId());

        template.requestBodyAndHeaders("direct:start", null, headers);

        await().until(new ListCallable(), Matchers.not(
                Matchers.hasItem(
                        Matchers.hasProperty("id", Matchers.equalTo(created.getId()))
                )
        ));
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                from("direct:start").to(String.format("openstack-keystone:%s?subsystem=regions&username=%s&password=%s&project=%s",
                        properties.getProperty("OPENSTACK_URI"),
                        properties.getProperty("OPENSTACK_USERNAME"),
                        properties.getProperty("OPENSTACK_PASSWORD"),
                        properties.getProperty("PROJECT_ID")));
            }
        };
    }

    private class ListCallable implements Callable<List<Region>> {

        @Override
        public List<Region> call() throws Exception {
            return (List<Region>) createClientForThread(osclient).identity().regions().list();
        }
    }
}
