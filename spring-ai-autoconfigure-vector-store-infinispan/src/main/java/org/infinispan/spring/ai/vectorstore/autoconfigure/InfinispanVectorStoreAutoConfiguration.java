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

package org.infinispan.spring.ai.vectorstore.autoconfigure;

import io.micrometer.observation.ObservationRegistry;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;

import org.infinispan.spring.ai.vectorstore.InfinispanVectorStore;
import org.infinispan.spring.starter.remote.InfinispanRemoteAutoConfiguration;
import org.infinispan.spring.starter.remote.InfinispanRemoteCacheCustomizer;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SpringAIVectorStoreTypes;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link AutoConfiguration Auto-configuration} for Infinispan Vector Store.
 *
 * @author Katia Aresti
 */
@AutoConfiguration(after = InfinispanRemoteAutoConfiguration.class)
@ConditionalOnClass({ InfinispanVectorStore.class, EmbeddingModel.class, RemoteCacheManager.class })
@ConditionalOnBean(RemoteCacheManager.class)
@EnableConfigurationProperties(InfinispanVectorStoreProperties.class)
@ConditionalOnProperty(name = SpringAIVectorStoreTypes.TYPE, havingValue = "infinispan", matchIfMissing = true)
public class InfinispanVectorStoreAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(InfinispanRemoteCacheCustomizer.class)
	public InfinispanRemoteCacheCustomizer protoStreamMarshallerCustomizer() {
		return builder -> builder.marshaller(new ProtoStreamMarshaller());
	}

	@Bean
	@ConditionalOnMissingBean
	public InfinispanVectorStore infinispanVectorStore(EmbeddingModel embeddingModel,
			InfinispanVectorStoreProperties properties, RemoteCacheManager infinispanClient,
			ObjectProvider<ObservationRegistry> observationRegistry,
			ObjectProvider<VectorStoreObservationConvention> customObservationConvention) {

		InfinispanVectorStore.Builder builder = InfinispanVectorStore.builder(infinispanClient, embeddingModel)
			.observationRegistry(observationRegistry.getIfAvailable())
			.customObservationConvention(customObservationConvention.getIfAvailable());

		if (properties.getCreateStore() != null) {
			builder.createStore(properties.getCreateStore());
		}
		if (properties.getRegisterSchema() != null) {
			builder.registerSchema(properties.getRegisterSchema());
		}
		if (properties.getSchemaFileName() != null) {
			builder.schemaFileName(properties.getSchemaFileName());
		}
		if (properties.getStoreName() != null) {
			builder.storeName(properties.getStoreName());
		}
		if (properties.getStoreConfig() != null) {
			builder.storeConfig(properties.getStoreConfig());
		}
		if (properties.getDistance() != null) {
			builder.distance(properties.getDistance());
		}
		if (properties.getSimilarity() != null) {
			builder.similarity(properties.getSimilarity());
		}
		if (properties.getPackageName() != null) {
			builder.packageName(properties.getPackageName());
		}
		if (properties.getItemName() != null) {
			builder.springAiItemName(properties.getItemName());
		}
		if (properties.getMetadataItemName() != null) {
			builder.metadataItemName(properties.getMetadataItemName());
		}

		return builder.build();
	}

}
