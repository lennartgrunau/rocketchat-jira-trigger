package se.gustavkarlsson.rocketchat.jira_trigger.jira;

import com.atlassian.jira.rest.client.api.AuthenticationHandler;
import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.MetadataRestClient;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import se.gustavkarlsson.rocketchat.jira_trigger.configuration.JiraConfiguration;

public class JiraModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(AuthenticationHandler.class).toProvider(AuthHandlerProvider.class);
	}

	@Provides
	JiraRestClient provideJiraRestClient(JiraConfiguration jiraConfig, AuthenticationHandler authHandler) {
		AsynchronousJiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
		return factory.create(jiraConfig.getUri(), authHandler);
	}

	@Provides
	MetadataRestClient provideMetadataRestClient(JiraRestClient jiraClient) {
		return jiraClient.getMetadataClient();
	}

	@Provides
	IssueRestClient provideIssueRestClient(JiraRestClient jiraClient) {
		return jiraClient.getIssueClient();
	}

}
