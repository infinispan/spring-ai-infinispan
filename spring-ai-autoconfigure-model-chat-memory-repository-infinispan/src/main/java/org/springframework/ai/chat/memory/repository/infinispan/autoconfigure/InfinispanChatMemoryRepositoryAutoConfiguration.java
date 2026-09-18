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

import org.infinispan.client.hotrod.RemoteCacheManager;

import org.springframework.ai.chat.memory.repository.infinispan.InfinispanChatMemoryRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link AutoConfiguration Auto-configuration} for Infinispan Chat Memory Repository.
 *
 * @author Katia Aresti
 */
@AutoConfiguration
@ConditionalOnClass({ InfinispanChatMemoryRepository.class, RemoteCacheManager.class })
@EnableConfigurationProperties(InfinispanChatMemoryRepositoryProperties.class)
public class InfinispanChatMemoryRepositoryAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public InfinispanChatMemoryRepository infinispanChatMemoryRepository(
			InfinispanChatMemoryRepositoryProperties properties, RemoteCacheManager remoteCacheManager) {

		InfinispanChatMemoryRepository.Builder builder = InfinispanChatMemoryRepository.builder(remoteCacheManager);

		if (properties.getCacheName() != null) {
			builder.cacheName(properties.getCacheName());
		}
		if (properties.getPackageName() != null) {
			builder.packageName(properties.getPackageName());
		}
		if (properties.getItemName() != null) {
			builder.itemName(properties.getItemName());
		}
		if (properties.getRegisterSchema() != null) {
			builder.registerSchema(properties.getRegisterSchema());
		}
		if (properties.getCreateCache() != null) {
			builder.createCache(properties.getCreateCache());
		}

		return builder.build();
	}

}
