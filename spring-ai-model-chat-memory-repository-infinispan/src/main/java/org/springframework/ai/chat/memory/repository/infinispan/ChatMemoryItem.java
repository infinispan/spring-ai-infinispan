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

package org.springframework.ai.chat.memory.repository.infinispan;

import org.jspecify.annotations.Nullable;

/**
 * Represents a chat memory message stored in Infinispan.
 *
 * @param id unique identifier for the message entry
 * @param conversationId the conversation this message belongs to
 * @param messageType the type of message (USER, ASSISTANT, SYSTEM, TOOL)
 * @param content the text content of the message
 * @param sequenceNumber the ordering of the message within the conversation
 * @param messageTimestamp epoch millis when the message was stored
 * @param metadata JSON-serialized metadata map
 * @param toolCalls JSON-serialized tool calls (for ASSISTANT messages)
 * @param toolResponses JSON-serialized tool responses (for TOOL messages)
 */
public record ChatMemoryItem(String id, String conversationId, String messageType, @Nullable String content,
		int sequenceNumber, long messageTimestamp, @Nullable String metadata, @Nullable String toolCalls,
		@Nullable String toolResponses) {

}
