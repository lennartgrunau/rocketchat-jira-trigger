package se.gustavkarlsson.rocketchat.jira_trigger.messages;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import se.gustavkarlsson.rocketchat.jira_trigger.configuration.MessageConfiguration;
import se.gustavkarlsson.rocketchat.jira_trigger.messages.field_creators.FieldCreator;

import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Singleton
class DefaultFieldCreatorsProvider implements Provider<List<FieldCreator>> {
	private final FieldCreatorMapper fieldCreatorMapper;
	private final MessageConfiguration messageConfig;

	@Inject
	DefaultFieldCreatorsProvider(FieldCreatorMapper fieldCreatorMapper, MessageConfiguration messageConfig) {
		this.fieldCreatorMapper = notNull(fieldCreatorMapper);
		this.messageConfig = notNull(messageConfig);
	}

	@Override
	public List<FieldCreator> get() {
		return fieldCreatorMapper.getCreators(messageConfig.getDefaultFields());
	}
}
