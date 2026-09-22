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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Infinispan Chat Memory Repository.
 *
 * @author Katia Aresti
 */
@ConfigurationProperties(prefix = InfinispanChatMemoryRepositoryProperties.CONFIG_PREFIX)
public class InfinispanChatMemoryRepositoryProperties {

	public static final String CONFIG_PREFIX = "spring.ai.chat.memory.repository.infinispan";

	private String cacheName;

	private String packageName;

	private String itemName;

	private Boolean registerSchema;

	private Boolean createCache;

	public String getCacheName() {
		return this.cacheName;
	}

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	public String getPackageName() {
		return this.packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getItemName() {
		return this.itemName;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}

	public Boolean getRegisterSchema() {
		return this.registerSchema;
	}

	public void setRegisterSchema(Boolean registerSchema) {
		this.registerSchema = registerSchema;
	}

	public Boolean getCreateCache() {
		return this.createCache;
	}

	public void setCreateCache(Boolean createCache) {
		this.createCache = createCache;
	}

}
