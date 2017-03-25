package org.apache.camel.component.openstack.it.cinder;

import static org.junit.Assert.assertNotNull;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.openstack.cinder.CinderConstants;
import org.apache.camel.component.openstack.it.AbstractITSupport;
import org.apache.camel.component.openstack.nova.NovaConstants;

import org.junit.Assert;
import org.junit.Test;

import org.hamcrest.Matchers;
import org.openstack4j.api.Builders;
import org.openstack4j.model.storage.block.Volume;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class VolumeIT extends AbstractITSupport {

	@Test
	public void createVolume() throws IOException, InterruptedException {

		Map<String, Object> headers = new HashMap();
		headers.put(CinderConstants.OPERATION, CinderConstants.CREATE);
		headers.put(CinderConstants.NAME, name);
		headers.put(CinderConstants.SIZE, 1);

		Volume created = template.requestBodyAndHeaders("direct:start", null, headers, Volume.class);

		Assert.assertEquals(name, created.getName());
		assertNotNull(created.getId());

		await().until(new ListCallable(), Matchers.hasItem(Matchers.allOf(Matchers.hasProperty("name", Matchers.equalTo(name)),
				Matchers.hasProperty("id", Matchers.equalTo(created.getId())))));
	}


	@Test
	public void getVol() {
		Volume created = osclient.blockStorage().volumes().create(Builders.volume().name(name).size(1).build());

		await().until(new ListCallable(), Matchers.hasItem(Matchers.allOf(Matchers.hasProperty("name", Matchers.equalTo(name)),
				Matchers.hasProperty("id", Matchers.equalTo(created.getId())))));

		Map<String, Object> headers = new HashMap();
		headers.put(NovaConstants.OPERATION, NovaConstants.GET);
		headers.put(NovaConstants.ID, created.getId());

		Volume k = template.requestBodyAndHeaders("direct:start", null, headers, Volume.class);
		Assert.assertEquals(k.getId(),created.getId());
		Assert.assertEquals(k.getName(),created.getName());
	}

	

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {
			public void configure() {
				from("direct:start").to(String.format("openstack-cinder:%s?subsystem=volumes&username=%s&password=%s&project=%s",
						properties.getProperty("OPENSTACK_URI"),
						properties.getProperty("OPENSTACK_USERNAME"),
						properties.getProperty("OPENSTACK_PASSWORD"),
						properties.getProperty("PROJECT_ID")));
			}
		};
	}

	private class ListCallable implements Callable<List<Volume>> {

		@Override
		public List<Volume> call() throws Exception {
			return (List<Volume>) createClientForThread(osclient).blockStorage().volumes().list();
		}
	}
}
