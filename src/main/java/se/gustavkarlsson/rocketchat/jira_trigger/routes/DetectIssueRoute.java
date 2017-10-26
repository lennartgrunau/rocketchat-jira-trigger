package se.gustavkarlsson.rocketchat.jira_trigger.routes;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.Issue;
import org.slf4j.Logger;
import se.gustavkarlsson.rocketchat.jira_trigger.messages.AttachmentCreator;
import se.gustavkarlsson.rocketchat.jira_trigger.messages.ToRocketChatMessageFactory;
import se.gustavkarlsson.rocketchat.jira_trigger.validation.Validator;
import se.gustavkarlsson.rocketchat.models.from_rocket_chat.FromRocketChatMessage;
import se.gustavkarlsson.rocketchat.models.to_rocket_chat.ToRocketChatAttachment;
import se.gustavkarlsson.rocketchat.models.to_rocket_chat.ToRocketChatMessage;
import se.gustavkarlsson.rocketchat.spark.routes.RocketChatMessageRoute;
import spark.Request;
import spark.Response;

import javax.inject.Inject;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.Validate.noNullElements;
import static org.apache.commons.lang3.Validate.notNull;
import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404;
import static org.slf4j.LoggerFactory.getLogger;

public class DetectIssueRoute extends RocketChatMessageRoute {
	private static final Logger log = getLogger(DetectIssueRoute.class);

	private final List<Validator> validators;
	private final JiraKeyParser jiraKeyParser;
	private final IssueRestClient issueClient;
	private final ToRocketChatMessageFactory messageFactory;
	private final AttachmentCreator attachmentCreator;

	@Inject
	public DetectIssueRoute(List<Validator> validators, JiraKeyParser jiraKeyParser, IssueRestClient issueClient,
							ToRocketChatMessageFactory messageFactory, AttachmentCreator attachmentCreator) {
		this.validators = noNullElements(validators);
		this.jiraKeyParser = notNull(jiraKeyParser);
		this.issueClient = notNull(issueClient);
		this.messageFactory = notNull(messageFactory);
		this.attachmentCreator = notNull(attachmentCreator);
	}

	@Override
	protected ToRocketChatMessage handle(Request request, Response response, FromRocketChatMessage fromRocketChatMessage) {
		log.debug("Validating message");
		if (!isValid(fromRocketChatMessage)) {
			log.info("Validation failed. Ignoring");
			return null;
		}

		log.debug("Parsing keys from text: '{}'", fromRocketChatMessage.getText());
		Map<String, IssueDetail> jiraKeys = jiraKeyParser.parse(fromRocketChatMessage.getText());
		if (jiraKeys.isEmpty()) {
			log.info("No keys found. Ignoring");
			return null;
		}
		log.info("Identified {} keys", jiraKeys.size());
		log.debug("Keys: {}", jiraKeys.keySet());

		log.debug("Fetching issues...");
		Map<Issue, IssueDetail> issues = getIssues(jiraKeys);
		log.info("Found {} issues", issues.size());
		if (issues.isEmpty()) {
			log.info("No issues found. Ignoring");
			return null;
		}
		log.debug("Issues: {}", issues.keySet().stream().map(Issue::getId).collect(toList()));
		log.debug("Creating message");
		ToRocketChatMessage message = messageFactory.create();
		message.setText(issues.size() == 1 ? "Found 1 issue" : "Found " + issues.size() + " issues");
		List<ToRocketChatAttachment> attachments = createAttachments(issues);
		message.setAttachments(attachments);
		return message;
	}

	private boolean isValid(FromRocketChatMessage fromRocketChatMessage) {
		return validators.stream().allMatch(validator -> validator.isValid(fromRocketChatMessage));
	}

	private Map<Issue, IssueDetail> getIssues(Map<String, IssueDetail> jiraKeys) {
		return jiraKeys.entrySet().parallelStream()
				.map(e -> new SimpleEntry<>(getIssue(e.getKey()), e.getValue()))
				.filter(e -> e.getKey() != null)
				.collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue));
	}

	private Issue getIssue(String jiraKey) {
		try {
			return issueClient.getIssue(jiraKey).claim();
		} catch (RestClientException e) {
			if (e.getStatusCode().or(0) != NOT_FOUND_404) {
				log.error("Jira client error", e);
			}
			return null;
		}
	}

	private List<ToRocketChatAttachment> createAttachments(Map<Issue, IssueDetail> issues) {
		return issues.entrySet().stream()
				.map(e -> attachmentCreator.create(e.getKey(), e.getValue()))
				.collect(toList());
	}
}
