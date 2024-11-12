/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.protocol.packet.chat.session;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.UUID;

public class PlayerSessionPacket implements MinecraftPacket {
  UUID sessionId;
  Long expiresAt;
  byte[] publicKey;
  byte[] keySignature;

  PlayerSessionPacket(UUID sessionId, Long expiresAt, byte[] publicKey, byte[] keySignature) {
    this.sessionId = sessionId;
    this.expiresAt = expiresAt;
    this.publicKey = publicKey;
    this.keySignature = keySignature;
  }

  public PlayerSessionPacket() {}

  public byte[] getKeySignature() {
    return keySignature;
  }

  public byte[] getPublicKey() {
    return publicKey;
  }

  public long getExpiresAt() {
    return expiresAt;
  }

  public UUID getSessionId() {
    return sessionId;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    this.sessionId = ProtocolUtils.readUuid(buf);
    this.expiresAt = buf.readLong();
    this.publicKey = ProtocolUtils.readByteArray(buf, 512);
    this.keySignature = ProtocolUtils.readByteArray(buf, 4096);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeUuid(buf, this.sessionId);
    buf.writeLong(this.expiresAt);
    ProtocolUtils.writeByteArray(buf, this.publicKey);
    ProtocolUtils.writeByteArray(buf, this.keySignature);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }


  @Override
  public String toString() {
    return "SessionPlayerCommand{"
        + "sessionId='" + sessionId + '\''
        + ", expiresAt=" + expiresAt
        + ", publicKey=" + Arrays.toString(publicKey)
        + ", keySignature=" + Arrays.toString(keySignature)
        + '}';
  }
}
