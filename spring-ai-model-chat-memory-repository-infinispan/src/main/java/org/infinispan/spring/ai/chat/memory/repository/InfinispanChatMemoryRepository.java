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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.api.query.Query;
import org.infinispan.commons.configuration.StringConfiguration;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.schema.Schema;
import org.infinispan.protostream.schema.Type;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * An implementation of {@link ChatMemoryRepository} backed by Infinispan.
 *
 * @author Katia Aresti
 */
public class InfinispanChatMemoryRepository implements ChatMemoryRepository, InitializingBean {

	public static final String DEFAULT_CACHE_NAME = "chat_memory";

	public static final String DEFAULT_PACKAGE = "dev.spring_ai";

	public static final String DEFAULT_ITEM_NAME = "ChatMemoryItem";

	private static final String DEFAULT_CACHE_CONFIG = "<distributed-cache name=\"CACHE_NAME\">\n"
			+ "<indexing storage=\"local-heap\">\n" + "<indexed-entities>\n"
			+ "<indexed-entity>CHAT_MEMORY_ITEM</indexed-entity>\n" + "</indexed-entities>\n" + "</indexing>\n"
			+ "</distributed-cache>";

	private static final Logger logger = LoggerFactory.getLogger(InfinispanChatMemoryRepository.class);

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private final RemoteCacheManager remoteCacheManager;

	private final String cacheName;

	private final String packageName;

	private final String itemName;

	private final String itemFullName;

	private final boolean registerSchema;

	private final boolean createCache;

	private @Nullable RemoteCache<String, ChatMemoryItem> remoteCache;

	private InfinispanChatMemoryRepository(Builder builder) {
		Assert.notNull(builder.remoteCacheManager, "remoteCacheManager must not be null");
		this.remoteCacheManager = builder.remoteCacheManager;
		this.cacheName = builder.cacheName != null ? builder.cacheName : DEFAULT_CACHE_NAME;
		this.packageName = builder.packageName != null ? builder.packageName : DEFAULT_PACKAGE;
		this.itemName = builder.itemName != null ? builder.itemName : DEFAULT_ITEM_NAME;
		this.registerSchema = builder.registerSchema != null ? builder.registerSchema : true;
		this.createCache = builder.createCache != null ? builder.createCache : true;
		this.itemFullName = this.packageName + "." + this.itemName;
	}

	public static Builder builder(RemoteCacheManager remoteCacheManager) {
		return new Builder(remoteCacheManager);
	}

	@Override
	public void afterPropertiesSet() {
		String schemaFileName = this.packageName + ".chat_memory.proto";

		Schema schema = new Schema.Builder(schemaFileName).packageName(this.packageName)
			.addMessage(this.itemName)
			.addComment("@Indexed")
			.addField(Type.Scalar.STRING, "id", 1)
			.addComment("@Basic(projectable=true)")
			.addField(Type.Scalar.STRING, "conversationId", 2)
			.addComment("@Basic(projectable=true)")
			.addField(Type.Scalar.STRING, "messageType", 3)
			.addComment("@Basic(projectable=true)")
			.addField(Type.Scalar.STRING, "content", 4)
			.addComment("@Basic(projectable=true)")
			.addField(Type.Scalar.INT32, "sequenceNumber", 5)
			.addComment("@Basic(projectable=true, sortable=true)")
			.addField(Type.Scalar.INT64, "messageTimestamp", 6)
			.addComment("@Basic(projectable=true, sortable=true)")
			.addField(Type.Scalar.STRING, "metadata", 7)
			.addComment("@Basic(projectable=true)")
			.addField(Type.Scalar.STRING, "toolCalls", 8)
			.addComment("@Basic(projectable=true)")
			.addField(Type.Scalar.STRING, "toolResponses", 9)
			.addComment("@Basic(projectable=true)")
			.build();

		ProtoStreamMarshaller marshaller = (ProtoStreamMarshaller) this.remoteCacheManager.getMarshallerRegistry()
			.getMarshaller(ProtoStreamMarshaller.class);
		if (marshaller == null) {
			throw new IllegalStateException("ProtoStreamMarshaller not found");
		}

		marshaller.register(schema, new ChatMemoryItemMarshaller(this.itemFullName));

		if (this.registerSchema) {
			this.remoteCacheManager.administration().schemas().createOrUpdate(schema);
		}

		this.remoteCacheManager.getConfiguration().addRemoteCache(this.cacheName, c -> c.marshaller(marshaller));

		this.remoteCache = this.remoteCacheManager.getCache(this.cacheName);
		if (this.remoteCache == null && this.createCache) {
			String cacheConfig = DEFAULT_CACHE_CONFIG.replace("CACHE_NAME", this.cacheName)
				.replace("CHAT_MEMORY_ITEM", this.itemFullName);
			this.remoteCache = this.remoteCacheManager.administration()
				.getOrCreateCache(this.cacheName, new StringConfiguration(cacheConfig));
		}

		if (this.remoteCache == null) {
			throw new IllegalStateException("Infinispan Cache '" + this.cacheName + "' not found");
		}
	}

	@Override
	public List<String> findConversationIds() {
		Assert.notNull(this.remoteCache, "remoteCache must not be null");
		String ickle = "SELECT DISTINCT c.conversationId FROM " + this.itemFullName + " c";
		Query<Object[]> query = this.remoteCache.query(ickle);
		List<Object[]> results = query.list();
		return results.stream().map(row -> (String) row[0]).collect(Collectors.toList());
	}

	@Override
	public List<Message> findByConversationId(String conversationId) {
		Assert.notNull(this.remoteCache, "remoteCache must not be null");
		Assert.hasText(conversationId, "conversationId cannot be null or empty");

		String ickle = "FROM " + this.itemFullName + " c WHERE c.conversationId = :conversationId ORDER BY c.sequenceNumber ASC";
		Query<ChatMemoryItem> query = this.remoteCache.query(ickle);
		query.setParameter("conversationId", conversationId);
		List<ChatMemoryItem> items = query.list();

		return items.stream().map(this::toMessage).collect(Collectors.toList());
	}

	@Override
	public void saveAll(String conversationId, List<Message> messages) {
		Assert.notNull(this.remoteCache, "remoteCache must not be null");
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.notNull(messages, "messages cannot be null");

		deleteByConversationId(conversationId);

		long timestamp = Instant.now().toEpochMilli();
		Map<String, ChatMemoryItem> entries = new HashMap<>(messages.size());

		for (int i = 0; i < messages.size(); i++) {
			Message message = messages.get(i);
			String id = UUID.randomUUID().toString();
			ChatMemoryItem item = fromMessage(id, conversationId, message, i, timestamp);
			entries.put(id, item);
		}

		this.remoteCache.putAll(entries);
	}

	@Override
	public void deleteByConversationId(String conversationId) {
		Assert.notNull(this.remoteCache, "remoteCache must not be null");
		Assert.hasText(conversationId, "conversationId cannot be null or empty");

		String ickle = "DELETE FROM " + this.itemFullName + " c WHERE c.conversationId = :conversationId";
		Query<ChatMemoryItem> query = this.remoteCache.query(ickle);
		query.setParameter("conversationId", conversationId);
		query.execute();
	}

	public void clear() {
		Assert.notNull(this.remoteCache, "remoteCache must not be null");
		this.remoteCache.clear();
	}

	private ChatMemoryItem fromMessage(String id, String conversationId, Message message, int sequenceNumber,
			long timestamp) {
		String metadata = null;
		if (message.getMetadata() != null && !message.getMetadata().isEmpty()) {
			metadata = toJson(message.getMetadata());
		}

		String toolCalls = null;
		if (message instanceof AssistantMessage assistantMessage && assistantMessage.hasToolCalls()) {
			List<Map<String, String>> toolCallList = new ArrayList<>();
			for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
				Map<String, String> tc = new HashMap<>();
				tc.put("id", toolCall.id());
				tc.put("type", toolCall.type());
				tc.put("name", toolCall.name());
				tc.put("arguments", toolCall.arguments());
				toolCallList.add(tc);
			}
			toolCalls = toJson(toolCallList);
		}

		String toolResponses = null;
		if (message instanceof ToolResponseMessage toolResponseMessage) {
			List<Map<String, String>> responseList = new ArrayList<>();
			for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
				Map<String, String> tr = new HashMap<>();
				tr.put("id", response.id());
				tr.put("name", response.name());
				tr.put("responseData", response.responseData());
				responseList.add(tr);
			}
			toolResponses = toJson(responseList);
		}

		return new ChatMemoryItem(id, conversationId, message.getMessageType().name(), message.getText(),
				sequenceNumber, timestamp, metadata, toolCalls, toolResponses);
	}

	private Message toMessage(ChatMemoryItem item) {
		MessageType messageType = MessageType.valueOf(item.messageType());
		Map<String, Object> metadata = item.metadata() != null ? fromJson(item.metadata()) : new HashMap<>();

		return switch (messageType) {
			case ASSISTANT -> {
				var builder = AssistantMessage.builder()
					.content(item.content() != null ? item.content() : "")
					.properties(metadata);
				if (item.toolCalls() != null) {
					List<Map<String, String>> toolCallDocs = fromJsonList(item.toolCalls());
					if (!toolCallDocs.isEmpty()) {
						List<AssistantMessage.ToolCall> toolCalls = toolCallDocs.stream()
							.map(tc -> new AssistantMessage.ToolCall(tc.get("id"), tc.get("type"), tc.get("name"),
									tc.get("arguments")))
							.collect(Collectors.toList());
						builder.toolCalls(toolCalls);
					}
				}
				yield builder.build();
			}
			case USER -> UserMessage.builder().text(item.content()).metadata(metadata).build();
			case SYSTEM -> SystemMessage.builder().text(item.content()).metadata(metadata).build();
			case TOOL -> {
				var builder = ToolResponseMessage.builder().metadata(metadata);
				if (item.toolResponses() != null) {
					List<Map<String, String>> responseDocs = fromJsonList(item.toolResponses());
					if (!responseDocs.isEmpty()) {
						List<ToolResponseMessage.ToolResponse> responses = responseDocs.stream()
							.map(tr -> new ToolResponseMessage.ToolResponse(tr.get("id"), tr.get("name"),
									tr.get("responseData")))
							.collect(Collectors.toList());
						builder.responses(responses);
					}
					else {
						builder.responses(List.of());
					}
				}
				else {
					builder.responses(List.of());
				}
				yield builder.build();
			}
			default -> throw new IllegalStateException("Unknown message type: " + item.messageType());
		};
	}

	private static String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		}
		catch (JacksonException e) {
			throw new IllegalStateException("Failed to serialize to JSON", e);
		}
	}

	private static Map<String, Object> fromJson(String json) {
		try {
			return objectMapper.readValue(json, new TypeReference<>() {
			});
		}
		catch (JacksonException e) {
			logger.warn("Failed to deserialize metadata JSON, returning empty map", e);
			return new HashMap<>();
		}
	}

	private static List<Map<String, String>> fromJsonList(String json) {
		try {
			return objectMapper.readValue(json, new TypeReference<>() {
			});
		}
		catch (JacksonException e) {
			logger.warn("Failed to deserialize JSON list, returning empty list", e);
			return Collections.emptyList();
		}
	}

	public static class Builder {

		private final RemoteCacheManager remoteCacheManager;

		@Nullable
		private String cacheName;

		@Nullable
		private String packageName;

		@Nullable
		private String itemName;

		@Nullable
		private Boolean registerSchema;

		@Nullable
		private Boolean createCache;

		public Builder(RemoteCacheManager remoteCacheManager) {
			Assert.notNull(remoteCacheManager, "remoteCacheManager must not be null");
			this.remoteCacheManager = remoteCacheManager;
		}

		public Builder cacheName(String cacheName) {
			this.cacheName = cacheName;
			return this;
		}

		public Builder packageName(String packageName) {
			this.packageName = packageName;
			return this;
		}

		public Builder itemName(String itemName) {
			this.itemName = itemName;
			return this;
		}

		public Builder registerSchema(Boolean registerSchema) {
			this.registerSchema = registerSchema;
			return this;
		}

		public Builder createCache(Boolean createCache) {
			this.createCache = createCache;
			return this;
		}

		public InfinispanChatMemoryRepository build() {
			return new InfinispanChatMemoryRepository(this);
		}

	}

}
