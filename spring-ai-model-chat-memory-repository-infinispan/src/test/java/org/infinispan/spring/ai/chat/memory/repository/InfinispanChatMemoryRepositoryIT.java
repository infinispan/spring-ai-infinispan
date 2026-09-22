/*
 * Copyright 2023-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.infinispan.spring.ai.chat.memory.repository;

import java.util.List;
import java.util.Map;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.util.Version;
import org.infinispan.testcontainers.InfinispanContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link InfinispanChatMemoryRepository}.
 *
 * @author Katia Aresti
 */
@Testcontainers
class InfinispanChatMemoryRepositoryIT {

	@Container
	static InfinispanContainer infinispanContainer = new InfinispanContainer(
			InfinispanContainer.IMAGE_BASENAME + ":" + Version.getVersion());

	private InfinispanChatMemoryRepository repository;

	@BeforeEach
	void setUp() {
		RemoteCacheManager cacheManager = new RemoteCacheManager(infinispanContainer.getConnectionURI());
		this.repository = InfinispanChatMemoryRepository.builder(cacheManager).build();
		this.repository.afterPropertiesSet();
		this.repository.clear();
	}

	@Test
	void saveAndRetrieveUserMessage() {
		String conversationId = "conv-1";
		List<Message> messages = List.of(UserMessage.builder().text("Hello, how are you?").build());

		this.repository.saveAll(conversationId, messages);

		List<Message> retrieved = this.repository.findByConversationId(conversationId);
		assertThat(retrieved).hasSize(1);
		assertThat(retrieved.get(0).getMessageType()).isEqualTo(MessageType.USER);
		assertThat(retrieved.get(0).getText()).isEqualTo("Hello, how are you?");
	}

	@Test
	void saveAndRetrieveAssistantMessage() {
		String conversationId = "conv-2";
		List<Message> messages = List.of(AssistantMessage.builder().content("I'm doing well!").build());

		this.repository.saveAll(conversationId, messages);

		List<Message> retrieved = this.repository.findByConversationId(conversationId);
		assertThat(retrieved).hasSize(1);
		assertThat(retrieved.get(0).getMessageType()).isEqualTo(MessageType.ASSISTANT);
		assertThat(retrieved.get(0).getText()).isEqualTo("I'm doing well!");
	}

	@Test
	void saveAndRetrieveSystemMessage() {
		String conversationId = "conv-3";
		List<Message> messages = List.of(SystemMessage.builder().text("You are a helpful assistant.").build());

		this.repository.saveAll(conversationId, messages);

		List<Message> retrieved = this.repository.findByConversationId(conversationId);
		assertThat(retrieved).hasSize(1);
		assertThat(retrieved.get(0).getMessageType()).isEqualTo(MessageType.SYSTEM);
		assertThat(retrieved.get(0).getText()).isEqualTo("You are a helpful assistant.");
	}

	@Test
	void saveAndRetrieveMultipleMessages() {
		String conversationId = "conv-4";
		List<Message> messages = List.of(SystemMessage.builder().text("You are a helpful assistant.").build(),
				UserMessage.builder().text("What is Spring AI?").build(),
				AssistantMessage.builder().content("Spring AI is a framework for AI integration.").build());

		this.repository.saveAll(conversationId, messages);

		List<Message> retrieved = this.repository.findByConversationId(conversationId);
		assertThat(retrieved).hasSize(3);
		assertThat(retrieved.get(0).getMessageType()).isEqualTo(MessageType.SYSTEM);
		assertThat(retrieved.get(1).getMessageType()).isEqualTo(MessageType.USER);
		assertThat(retrieved.get(2).getMessageType()).isEqualTo(MessageType.ASSISTANT);
	}

	@Test
	void saveReplacesExistingMessages() {
		String conversationId = "conv-5";
		this.repository.saveAll(conversationId,
				List.of(UserMessage.builder().text("First message").build()));

		List<Message> retrieved = this.repository.findByConversationId(conversationId);
		assertThat(retrieved).hasSize(1);
		assertThat(retrieved.get(0).getText()).isEqualTo("First message");

		this.repository.saveAll(conversationId,
				List.of(UserMessage.builder().text("Replacement message").build(),
						AssistantMessage.builder().content("Reply").build()));

		retrieved = this.repository.findByConversationId(conversationId);
		assertThat(retrieved).hasSize(2);
		assertThat(retrieved.get(0).getText()).isEqualTo("Replacement message");
		assertThat(retrieved.get(1).getText()).isEqualTo("Reply");
	}

	@Test
	void deleteByConversationId() {
		String conversationId = "conv-6";
		this.repository.saveAll(conversationId,
				List.of(UserMessage.builder().text("To be deleted").build()));

		assertThat(this.repository.findByConversationId(conversationId)).hasSize(1);

		this.repository.deleteByConversationId(conversationId);

		assertThat(this.repository.findByConversationId(conversationId)).isEmpty();
	}

	@Test
	void findConversationIds() {
		this.repository.saveAll("conv-a",
				List.of(UserMessage.builder().text("Hello from A").build()));
		this.repository.saveAll("conv-b",
				List.of(UserMessage.builder().text("Hello from B").build()));
		this.repository.saveAll("conv-c",
				List.of(UserMessage.builder().text("Hello from C").build()));

		List<String> ids = this.repository.findConversationIds();
		assertThat(ids).containsExactlyInAnyOrder("conv-a", "conv-b", "conv-c");
	}

	@Test
	void findByNonExistentConversation() {
		List<Message> messages = this.repository.findByConversationId("non-existent");
		assertThat(messages).isEmpty();
	}

	@Test
	void saveAndRetrieveAssistantMessageWithToolCalls() {
		String conversationId = "conv-tools";
		AssistantMessage assistantMessage = AssistantMessage.builder()
			.content("")
			.toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", "get_weather",
					"{\"location\": \"Paris\"}")))
			.build();

		this.repository.saveAll(conversationId, List.of(assistantMessage));

		List<Message> retrieved = this.repository.findByConversationId(conversationId);
		assertThat(retrieved).hasSize(1);
		assertThat(retrieved.get(0)).isInstanceOf(AssistantMessage.class);
		AssistantMessage result = (AssistantMessage) retrieved.get(0);
		assertThat(result.hasToolCalls()).isTrue();
		assertThat(result.getToolCalls()).hasSize(1);
		assertThat(result.getToolCalls().get(0).name()).isEqualTo("get_weather");
		assertThat(result.getToolCalls().get(0).arguments()).isEqualTo("{\"location\": \"Paris\"}");
	}

	@Test
	void saveAndRetrieveToolResponseMessage() {
		String conversationId = "conv-tool-response";
		ToolResponseMessage toolResponse = ToolResponseMessage.builder()
			.responses(List.of(new ToolResponseMessage.ToolResponse("call-1", "get_weather", "Sunny, 25°C")))
			.build();

		this.repository.saveAll(conversationId, List.of(toolResponse));

		List<Message> retrieved = this.repository.findByConversationId(conversationId);
		assertThat(retrieved).hasSize(1);
		assertThat(retrieved.get(0)).isInstanceOf(ToolResponseMessage.class);
		ToolResponseMessage result = (ToolResponseMessage) retrieved.get(0);
		assertThat(result.getResponses()).hasSize(1);
		assertThat(result.getResponses().get(0).name()).isEqualTo("get_weather");
		assertThat(result.getResponses().get(0).responseData()).isEqualTo("Sunny, 25°C");
	}

	@Test
	void saveAndRetrieveMessageWithMetadata() {
		String conversationId = "conv-metadata";
		UserMessage message = UserMessage.builder()
			.text("Hello")
			.metadata(Map.of("source", "web", "userId", "user-123"))
			.build();

		this.repository.saveAll(conversationId, List.of(message));

		List<Message> retrieved = this.repository.findByConversationId(conversationId);
		assertThat(retrieved).hasSize(1);
		assertThat(retrieved.get(0).getMetadata()).containsEntry("source", "web");
		assertThat(retrieved.get(0).getMetadata()).containsEntry("userId", "user-123");
	}

}
