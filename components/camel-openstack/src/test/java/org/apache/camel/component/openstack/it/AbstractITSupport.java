package org.apache.camel.component.openstack.it;

import org.apache.camel.test.junit4.CamelTestSupport;

import org.junit.Before;
import org.junit.BeforeClass;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.awaitility.core.ConditionFactory;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

public class AbstractITSupport extends CamelTestSupport {
	protected static final Duration TIMEOUT = Duration.ONE_MINUTE;
	protected static Properties properties;

	protected OSClient.OSClientV3 osclient;
	protected String name = UUID.randomUUID().toString();

	@BeforeClass
	public static void beforeClass() throws IOException {
		properties = new Properties();
		properties.load(AbstractITSupport.class.getClassLoader().getResourceAsStream("OpenstackTestProperties.properties"));
	}

	@Before
	public void before() {
		osclient = OSFactory.builderV3().endpoint(properties.getProperty("OPENSTACK_URI"))
				.credentials(properties.getProperty("OPENSTACK_USERNAME"), properties.getProperty("OPENSTACK_PASSWORD"), Identifier.byId("default"))
				.scopeToProject(Identifier.byId(properties.getProperty("PROJECT_ID"))).authenticate();
	}

	protected static OSClient.OSClientV3 createClientForThread(OSClient.OSClientV3 osClient) {
		return OSFactory.clientFromToken(osClient.getToken());
	}

	protected static ConditionFactory await() {
		return Awaitility.await().atMost(TIMEOUT).pollDelay(Duration.FIVE_SECONDS).pollInterval(Duration.ONE_SECOND);
	}
}
