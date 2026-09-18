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

package org.springframework.ai.chat.memory.repository.infinispan.autoconfigure;

import java.util.List;

import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.commons.util.Version;
import org.infinispan.spring.starter.remote.InfinispanRemoteAutoConfiguration;
import org.infinispan.testcontainers.InfinispanContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.ai.chat.memory.repository.infinispan.InfinispanChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link InfinispanChatMemoryRepositoryAutoConfiguration}.
 *
 * @author Katia Aresti
 */
@Testcontainers
class InfinispanChatMemoryRepositoryAutoConfigurationIT {

	@Container
	private static final InfinispanContainer infinispanContainer = new InfinispanContainer(
			InfinispanContainer.IMAGE_BASENAME + ":" + Version.getVersion());

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(InfinispanRemoteAutoConfiguration.class,
				InfinispanChatMemoryRepositoryAutoConfiguration.class))
		.withPropertyValues("infinispan.remote.server-list=" + serverList(),
				"infinispan.remote.auth-username=" + InfinispanContainer.DEFAULT_USERNAME,
				"infinispan.remote.marshaller=" + ProtoStreamMarshaller.class.getName(),
				"infinispan.remote.auth-password=" + InfinispanContainer.DEFAULT_PASSWORD);

	@Test
	void autoConfigurationCreatesBean() {
		this.contextRunner.run(context -> {
			assertThat(context.getBeansOfType(InfinispanChatMemoryRepository.class)).isNotEmpty();
		});
	}

	@Test
	void saveAndRetrieveMessages() {
		this.contextRunner.run(context -> {
			InfinispanChatMemoryRepository repository = context.getBean(InfinispanChatMemoryRepository.class);

			repository.saveAll("test-conv",
					List.of(UserMessage.builder().text("Hello from autoconfigure test").build()));

			List<Message> messages = repository.findByConversationId("test-conv");
			assertThat(messages).hasSize(1);
			assertThat(messages.get(0).getMessageType()).isEqualTo(MessageType.USER);
			assertThat(messages.get(0).getText()).isEqualTo("Hello from autoconfigure test");

			repository.deleteByConversationId("test-conv");
		});
	}

	@Test
	void propertiesTest() {
		this.contextRunner
			.withPropertyValues("spring.ai.chat.memory.repository.infinispan.cache-name=custom_memory",
					"spring.ai.chat.memory.repository.infinispan.package-name=custom.pkg",
					"spring.ai.chat.memory.repository.infinispan.item-name=CustomItem")
			.run(context -> {
				var properties = context.getBean(InfinispanChatMemoryRepositoryProperties.class);
				assertThat(properties.getCacheName()).isEqualTo("custom_memory");
				assertThat(properties.getPackageName()).isEqualTo("custom.pkg");
				assertThat(properties.getItemName()).isEqualTo("CustomItem");

				InfinispanChatMemoryRepository repository = context.getBean(InfinispanChatMemoryRepository.class);
				assertThat(repository).isNotNull();
			});
	}

	@Test
	void findConversationIds() {
		this.contextRunner.run(context -> {
			InfinispanChatMemoryRepository repository = context.getBean(InfinispanChatMemoryRepository.class);

			repository.saveAll("conv-1",
					List.of(UserMessage.builder().text("msg 1").build()));
			repository.saveAll("conv-2",
					List.of(UserMessage.builder().text("msg 2").build()));

			List<String> ids = repository.findConversationIds();
			assertThat(ids).containsExactlyInAnyOrder("conv-1", "conv-2");

			repository.deleteByConversationId("conv-1");
			repository.deleteByConversationId("conv-2");
		});
	}

	private static String serverList() {
		return infinispanContainer.getHost() + ":"
				+ infinispanContainer.getMappedPort(InfinispanContainer.DEFAULT_HOTROD_PORT);
	}

}
