package org.apache.camel.component.openstack.it.swift;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.openstack.it.AbstractITSupport;
import org.apache.camel.component.openstack.swift.SwiftConstants;

import org.junit.Before;
import org.junit.Test;

import org.hamcrest.Matchers;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.storage.object.SwiftObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

public class ObjectIT extends AbstractITSupport {

	private String containerName;

	@Before
	public void setUpTest() throws InterruptedException {
		containerName = UUID.randomUUID().toString();
		osclient.objectStorage().containers().create(containerName);
	}

	@Test
	public void createSwiftObject() throws IOException, InterruptedException {

		Map<String, Object> headers = new HashMap();
		headers.put(SwiftConstants.OPERATION, SwiftConstants.CREATE);
		headers.put(SwiftConstants.CONTAINER_NAME, containerName);
		headers.put(SwiftConstants.OBJECT_NAME, name);

		final String payload = "mypayload";
		InputStream stream = new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8));
		String createdEtag = template.requestBodyAndHeaders("direct:start", stream, headers, String.class);
		assertNotNull(createdEtag);

		await().until(new ListCallable(containerName), Matchers.hasItem(Matchers.allOf(Matchers.<SwiftObject>hasProperty("name", Matchers.equalTo(name)),
				Matchers.<SwiftObject>hasProperty("containerName", Matchers.equalTo(containerName)))));

		SwiftObject k = osclient.objectStorage().objects().get(containerName, name);
		assertNotNull(k.getLastModified());
		assertEquals(payload, new BufferedReader(new InputStreamReader(k.download().getInputStream())).readLine());
	}

	@Test
	public void get() {
		final InputStream stream = new ByteArrayInputStream("mypayload".getBytes(StandardCharsets.UTF_8));
		String et = osclient.objectStorage().objects().put(containerName, name, Payloads.create(stream));

		await().until(new ListCallable(containerName), Matchers.hasItem(Matchers.allOf(Matchers.<SwiftObject>hasProperty("name", Matchers.equalTo(name)),
				Matchers.<SwiftObject>hasProperty("containerName", Matchers.equalTo(containerName)))));

		Map<String, Object> headers = new HashMap();
		headers.put(SwiftConstants.OPERATION, SwiftConstants.GET);
		headers.put(SwiftConstants.CONTAINER_NAME, containerName);
		headers.put(SwiftConstants.OBJECT_NAME, name);

		SwiftObject get = template.requestBodyAndHeaders("direct:start", null, headers, SwiftObject.class);
		assertEquals(name, get.getName());
		assertTrue(get.getSizeInBytes() > 0);
	}

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {
			public void configure() {
				from("direct:start").to(String.format("openstack-swift:%s?subsystem=objects&username=%s&password=%s&project=%s",
						properties.getProperty("OPENSTACK_URI"),
						properties.getProperty("OPENSTACK_USERNAME"),
						properties.getProperty("OPENSTACK_PASSWORD"),
						properties.getProperty("PROJECT_ID")));
			}
		};
	}

	private class ListCallable implements Callable<List<SwiftObject>> {
		private String containerName;

		public ListCallable(String containerName) {
			this.containerName = containerName;
		}

		@Override
		public List<SwiftObject> call() throws Exception {
			return (List<SwiftObject>) createClientForThread(osclient).objectStorage().objects().list(containerName);
		}
	}
}
