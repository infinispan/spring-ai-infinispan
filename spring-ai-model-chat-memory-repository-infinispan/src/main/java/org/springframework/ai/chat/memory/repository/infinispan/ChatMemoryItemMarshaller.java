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

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;

/**
 * Protobuf marshaller for {@link ChatMemoryItem}.
 *
 * @author Katia Aresti
 */
public class ChatMemoryItemMarshaller implements MessageMarshaller<ChatMemoryItem> {

	private final String typeName;

	public ChatMemoryItemMarshaller(String typeName) {
		this.typeName = typeName;
	}

	@Override
	public ChatMemoryItem readFrom(ProtoStreamReader reader) throws IOException {
		String id = reader.readString("id");
		String conversationId = reader.readString("conversationId");
		String messageType = reader.readString("messageType");
		String content = reader.readString("content");
		Integer sequenceNumber = reader.readInt("sequenceNumber");
		Long messageTimestamp = reader.readLong("messageTimestamp");
		String metadata = reader.readString("metadata");
		String toolCalls = reader.readString("toolCalls");
		String toolResponses = reader.readString("toolResponses");

		return new ChatMemoryItem(id, conversationId, messageType, content,
				sequenceNumber != null ? sequenceNumber : 0, messageTimestamp != null ? messageTimestamp : 0L, metadata,
				toolCalls, toolResponses);
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, ChatMemoryItem item) throws IOException {
		writer.writeString("id", item.id());
		writer.writeString("conversationId", item.conversationId());
		writer.writeString("messageType", item.messageType());
		writer.writeString("content", item.content());
		writer.writeInt("sequenceNumber", item.sequenceNumber());
		writer.writeLong("messageTimestamp", item.messageTimestamp());
		writer.writeString("metadata", item.metadata());
		writer.writeString("toolCalls", item.toolCalls());
		writer.writeString("toolResponses", item.toolResponses());
	}

	@Override
	public Class<? extends ChatMemoryItem> getJavaClass() {
		return ChatMemoryItem.class;
	}

	@Override
	public String getTypeName() {
		return this.typeName;
	}

}
