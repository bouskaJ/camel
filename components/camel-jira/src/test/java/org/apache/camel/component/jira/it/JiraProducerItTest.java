package org.apache.camel.component.jira.it;

import static org.apache.camel.component.jira.JiraConstants.ISSUE_ASSIGNEE;
import static org.apache.camel.component.jira.JiraConstants.ISSUE_KEY;
import static org.apache.camel.component.jira.JiraConstants.ISSUE_PRIORITY_NAME;
import static org.apache.camel.component.jira.JiraConstants.ISSUE_PROJECT_KEY;
import static org.apache.camel.component.jira.JiraConstants.ISSUE_SUMMARY;
import static org.apache.camel.component.jira.JiraConstants.ISSUE_TYPE_NAME;

import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.direct.DirectEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.wiremock.CamelWiremockTestSupport;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.atlassian.jira.rest.client.api.domain.Issue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class JiraProducerItTest extends CamelWiremockTestSupport {

	@EndpointInject(uri = "mock:result")
	private MockEndpoint mockResult;

	@EndpointInject(uri = "direct:createIssue")
	private DirectEndpoint createIssue;

	@EndpointInject(uri = "direct:comment")
	private DirectEndpoint comment;

	private static final Properties properties = new Properties();

	@BeforeClass
	public static void loadProperties() throws IOException {
		properties.load(JiraProducerItTest.class.getResourceAsStream("/ItTestConfiguration.properties"));
	}

	@Test
	public void testCreateIssue() throws InterruptedException {
		mockResult.expectedMessageCount(1);

		Map<String, Object> headers = new HashMap<>();
		headers.put(ISSUE_PROJECT_KEY, properties.getProperty("project"));
		headers.put(ISSUE_TYPE_NAME, "Task");
		headers.put(ISSUE_SUMMARY, "Demo Bug jira");
		headers.put(ISSUE_PRIORITY_NAME, "Low");
		headers.put(ISSUE_ASSIGNEE, properties.getProperty("username"));
		template.sendBodyAndHeaders(createIssue, "Minha descrição jira", headers);

		mockResult.assertIsSatisfied();

		final Issue result = mockResult.getExchanges().get(0).getIn().getBody(Issue.class);
		Assert.assertEquals(properties.getProperty("project"), result.getProject().getKey());
		Assert.assertEquals(properties.getProperty("username"), result.getAssignee().getName());
	}

	@Test
	public void commentIssueTest() throws InterruptedException {
		mockResult.expectedMessageCount(1);
		template.sendBodyAndHeader(comment, "Comment from camel-jira IT test",  ISSUE_KEY, properties.getProperty("existing_issue"));

		mockResult.assertIsSatisfied();
	}

	@Override
	protected String mockedUrl() {
		return properties.getProperty("jira_url");
	}

	@Override
	protected RouteBuilder createRouteBuilder() {

		return new RouteBuilder() {
			@Override
			public void configure() {
				from(createIssue)
						.toF("jira://addIssue?jiraUrl=%s&username=%s&password=%s", getWiremockHostUrl(), properties.getProperty("username"), properties.getProperty("password"))
						.to(mockResult);

				from(comment)
						.toF("jira://addComment?jiraUrl=%s&username=%s&password=%s", getWiremockHostUrl(), properties.getProperty("username"), properties.getProperty("password"))
						.to(mockResult);
			}
		};
	}
}
