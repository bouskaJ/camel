package org.apache.camel.component.openstack.it.glance;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.openstack.glance.GlanceConstants;
import org.apache.camel.component.openstack.it.AbstractITSupport;
import org.apache.camel.component.openstack.nova.NovaConstants;

import org.junit.Assert;
import org.junit.Test;

import org.hamcrest.Matchers;
import org.openstack4j.api.Builders;
import org.openstack4j.model.image.ContainerFormat;
import org.openstack4j.model.image.DiskFormat;
import org.openstack4j.model.image.Image;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class GlanceIT extends AbstractITSupport {

	@Test
	public void reserveImage() {
		Map<String, Object> headers = new HashMap<>();
		headers.put(NovaConstants.OPERATION, GlanceConstants.RESERVE);
		headers.put(GlanceConstants.DISK_FORMAT, DiskFormat.ISO);
		headers.put(GlanceConstants.CONTAINER_FORMAT, ContainerFormat.BARE);
		headers.put(GlanceConstants.NAME, name);
		Image created = template.requestBodyAndHeaders("direct:start", null, headers, Image.class);

		Assert.assertNotNull(created.getId());

		await().until(new ListCallable(), Matchers.hasItem(Matchers.allOf(Matchers.hasProperty("name", Matchers.equalTo(name)),
				Matchers.hasProperty("id", Matchers.equalTo(created.getId())))));
	}

	@Test
	public void createImage() throws IOException {

		Map<String, Object> headers = new HashMap<>();
		headers.put(NovaConstants.OPERATION, GlanceConstants.CREATE);
		headers.put(GlanceConstants.DISK_FORMAT, DiskFormat.ISO);
		headers.put(GlanceConstants.CONTAINER_FORMAT, ContainerFormat.BARE);
		headers.put(GlanceConstants.NAME, name);
		Image created = template.requestBodyAndHeaders("direct:start", File.createTempFile("tmpfile", ".img"), headers, Image.class);

		Assert.assertNotNull(created.getId());

		await().until(new ListCallable(), Matchers.hasItem(Matchers.allOf(Matchers.hasProperty("name", Matchers.equalTo(name)),
				Matchers.hasProperty("id", Matchers.equalTo(created.getId())))));
	}

	@Test
	public void get() {
		Image model = Builders.image().name(name).diskFormat(DiskFormat.AKI).containerFormat(ContainerFormat.AKI).build();

		model = osclient.images().reserve(model);

		await().until(new ListCallable(), Matchers.hasItem(Matchers.allOf(Matchers.hasProperty("name", Matchers.equalTo(name)),
				Matchers.hasProperty("id", Matchers.equalTo(model.getId())))));

		Map<String, Object> headers = new HashMap();
		headers.put(NovaConstants.OPERATION, NovaConstants.GET);
		headers.put(NovaConstants.ID, model.getId());

		Image k = template.requestBodyAndHeaders("direct:start", null, headers, Image.class);
		Assert.assertEquals(k.getId(), model.getId());
		Assert.assertEquals(k.getName(), model.getName());
	}

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {
			public void configure() {
				from("direct:start").to(String.format("openstack-glance:%s?username=%s&password=%s&project=%s",
						properties.getProperty("OPENSTACK_URI"),
						properties.getProperty("OPENSTACK_USERNAME"),
						properties.getProperty("OPENSTACK_PASSWORD"),
						properties.getProperty("PROJECT_ID")));
			}
		};
	}

	private class ListCallable implements Callable<List<Image>> {

		@Override
		public List<Image> call() throws Exception {
			return (List<Image>) createClientForThread(osclient).images().list();
		}
	}
}
