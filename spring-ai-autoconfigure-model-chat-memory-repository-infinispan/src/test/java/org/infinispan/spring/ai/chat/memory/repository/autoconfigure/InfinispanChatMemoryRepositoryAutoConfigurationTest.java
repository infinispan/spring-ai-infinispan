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

package org.infinispan.spring.ai.chat.memory.repository.autoconfigure;

import org.infinispan.spring.ai.chat.memory.repository.InfinispanChatMemoryRepository;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InfinispanChatMemoryRepositoryAutoConfiguration}.
 *
 * @author Katia Aresti
 */
class InfinispanChatMemoryRepositoryAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(InfinispanChatMemoryRepositoryAutoConfiguration.class));

	@Test
	void shouldNotCreateBeanWhenRemoteCacheManagerIsMissing() {
		this.contextRunner.run(context -> {
			assertThat(context).doesNotHaveBean(InfinispanChatMemoryRepository.class);
		});
	}

}
