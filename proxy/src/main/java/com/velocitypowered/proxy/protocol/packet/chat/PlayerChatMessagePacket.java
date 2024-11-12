package com.velocitypowered.proxy.protocol.packet.chat;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Base64;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PlayerChatMessagePacket implements MinecraftPacket {
    UUID senderId;
    int index;
    byte @Nullable [] signature;

    String message;
    Long timestamp;
    Long salt;

    List<PreviousChatMessage> previousChatMessages;

    @Nullable
    String unsignedContent;
    FilterType filterType;
    @Nullable
    BitSet filterTypeBits;

    int chatType;
    BinaryTag senderName;
    @Nullable
    BinaryTag targetName;

    enum FilterType {
        pass_through,
        fully_filtered,
        partially_filtered
    }



    @Override
    public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        senderId = ProtocolUtils.readUuid(buf);
        index = ProtocolUtils.readVarInt(buf);

        boolean messageSignaturePresent = buf.readBoolean();
        if (messageSignaturePresent) {
            signature = new byte[256];
            buf.readBytes(signature);
        }

        message = ProtocolUtils.readString(buf, 256);
        timestamp = buf.readLong();
        salt = buf.readLong();

        int totalPreviousMessages = ProtocolUtils.readVarInt(buf);
        for (int i = 0; i < totalPreviousMessages; i++) {
            int messageId = ProtocolUtils.readVarInt(buf);

            byte[] signature = null;
            if(messageId == 0) {
                signature = new byte[256];
                buf.readBytes(signature);
            }

            previousChatMessages.add(new PreviousChatMessage(messageId, signature));
        }

        boolean unsignedContentPresent = buf.readBoolean();
        if (unsignedContentPresent) {
            unsignedContent = ProtocolUtils.readString(buf, 256);
        }

        int filterType = ProtocolUtils.readVarInt(buf);
        this.filterType = FilterType.values()[filterType];

        if (this.filterType == FilterType.partially_filtered) {
            int x = ProtocolUtils.readVarInt(buf);
            long[] bitSetLongs = new long[x];
            for (int i = 0; i < x; i++) {
                bitSetLongs[i] = buf.readLong();
            }
            filterTypeBits = BitSet.valueOf(bitSetLongs);
        }

        chatType = ProtocolUtils.readVarInt(buf);
        senderName = ProtocolUtils.readBinaryTag(buf, protocolVersion, BinaryTagIO.reader());
        boolean hasTargetName = buf.readBoolean();
        if (hasTargetName) {
            targetName = ProtocolUtils.readBinaryTag(buf, protocolVersion, BinaryTagIO.reader());
        }
    }

    @Override
    public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        ProtocolUtils.writeUuid(buf, senderId);
        ProtocolUtils.writeVarInt(buf, index);

        buf.writeBoolean(signature != null);
        if (signature != null) {
            buf.writeBytes(signature);
        }

        ProtocolUtils.writeString(buf, message);
        buf.writeLong(timestamp);
        buf.writeLong(salt);



        if (previousChatMessages == null) {
            ProtocolUtils.writeVarInt(buf, 0);
        } else {
            ProtocolUtils.writeVarInt(buf, previousChatMessages.size());
            for (PreviousChatMessage previousChatMessage : previousChatMessages) {
                ProtocolUtils.writeVarInt(buf, previousChatMessage.messageId);
                if (previousChatMessage.messageId == 0) {
                    buf.writeBytes(previousChatMessage.signature);
                }
            }
        }
        buf.writeBoolean(unsignedContent != null);
        if (unsignedContent != null) {
            ProtocolUtils.writeString(buf, unsignedContent);
        }

        ProtocolUtils.writeVarInt(buf, filterType.ordinal());
        if (filterType == FilterType.partially_filtered && filterTypeBits != null) {
            long[] longs = filterTypeBits.toLongArray();
            ProtocolUtils.writeVarInt(buf, longs.length);
            for (long l : longs) {
                buf.writeLong(l);
            }
        }
        ProtocolUtils.writeVarInt(buf, chatType);
        ProtocolUtils.writeBinaryTag(buf, protocolVersion, senderName);
        buf.writeBoolean(targetName != null);
        if (targetName != null) {
            ProtocolUtils.writeBinaryTag(buf, protocolVersion, targetName);
        }

        var bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
    }

    @Override
    public String toString() {
        return "PlayerChatMessagePacket{"
                + "sender=" + senderName + '\''
                + ", index=" + index
                + ", signature=" + (signature != null ? Base64.getEncoder().encodeToString(signature) : null)
                + ", message=" + message
                + ", timestamp=" + timestamp
                + ", salt=" + salt
                + ", previousChatMessages=" + previousChatMessages
                + ", unsignedContent=" + unsignedContent
                + ", filterType=" + filterType
                + ", filterTypeBits=" + filterTypeBits
                + ", chatType=" + chatType
                + ", senderName=" + senderName
                + ", targetName=" + targetName
                + '}';
    }

    @Override
    public boolean handle(MinecraftSessionHandler handler) {
        return handler.handle(this);
    }


    record PreviousChatMessage(int messageId, byte[] signature) {
        @Override
        public String toString() {
            return "PreviousChatMessage("
                    + "messageId=" + messageId + '\''
                    + "signature=" + Base64.getEncoder().encodeToString(signature);
        }
    }
}
