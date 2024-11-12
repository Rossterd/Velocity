/*
 * Copyright (C) 2022-2023 Velocity Contributors
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

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.proxy.crypto.IdentifiedKeyImpl;
import com.velocitypowered.proxy.crypto.MojangPublicKeys;
import static com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedChatHandler.invalidCancel;
import static com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedChatHandler.invalidChange;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.chat.ChatHandler;
import com.velocitypowered.proxy.protocol.packet.chat.ChatQueue;
import com.velocitypowered.proxy.protocol.packet.chat.RemoteChatSession;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;

public class SessionChatHandler implements ChatHandler<SessionPlayerChatPacket> {

  private static final Logger logger = LogManager.getLogger(SessionChatHandler.class);

  private final ConnectedPlayer player;
  private final VelocityServer server;

  public SessionChatHandler(ConnectedPlayer player, VelocityServer server) {
    this.player = player;
    this.server = server;
  }

  @Override
  public Class<SessionPlayerChatPacket> packetClass() {
    return SessionPlayerChatPacket.class;
  }

  @Override
  public void handlePlayerChatInternal(SessionPlayerChatPacket packet) {
    ChatQueue chatQueue = this.player.getChatQueue();
    EventManager eventManager = this.server.getEventManager();
    PlayerChatEvent toSend = new PlayerChatEvent(player, packet.getMessage());
    CompletableFuture<PlayerChatEvent> eventFuture = eventManager.fire(toSend);
    chatQueue.queuePacket(
        newLastSeenMessages -> eventFuture
            .thenApply(pme -> {
              PlayerChatEvent.ChatResult chatResult = pme.getResult();
              if (!chatResult.isAllowed()) {
                if (packet.isSigned()) {
                  invalidCancel(logger, player);
                }
                return null;
              }

              if (chatResult.getMessage().map(str -> !str.equals(packet.getMessage()))
                  .orElse(false)) {
                if (packet.isSigned()) {
                  invalidChange(logger, player);
                  return null;
                }
                return this.player.getChatBuilderFactory().builder()
                    .message(chatResult.getMessage().orElse(packet.getMessage()))
                    .setTimestamp(packet.timestamp)
                    .setLastSeenMessages(newLastSeenMessages)
                    .toServer();
              }
              return packet.withLastSeenMessages(newLastSeenMessages);
            })
            .exceptionally((ex) -> {
              logger.error("Exception while handling player chat for {}", player, ex);
              return null;
            }),
        packet.getTimestamp(),
        packet.getLastSeenMessages()
    );
  }

  @Override
  public boolean handlePlayerSession(PlayerSessionPacket packet) {
//    try {
//      // Line up the bytes to verify.
//      byte[] bytes = new byte[24 + packet.publicKey.length];
//      ByteBuffer.wrap(bytes)
//              .order(ByteOrder.BIG_ENDIAN)
//              .putLong(player.getUniqueId().getMostSignificantBits())
//              .putLong(player.getUniqueId().getLeastSignificantBits())
//              .putLong(packet.expiresAt)
//              .put(packet.publicKey);
//
//
//      // Verify bytes with signature.
//      Signature sign = Signature.getInstance("SHA1withRSA");
//      var mojangPublicKeys = server.getMojangPublicKeys().getPlayerCertificateKeys();
//
//      boolean verified = false;
//      for (PublicKey mojangPublicKey : mojangPublicKeys) {
//        sign.initVerify(mojangPublicKey);
//        sign.update(bytes);
//        if (sign.verify(packet.keySignature)) {
//          verified = true;
//        }
//      }
//
//      // Enforce verification of chat session info
//      if (!verified) {
//        // TODO: enforce chat signing? Kick em?
//        logger.warn("Player {} tried to initialize player session with unsigned details.", player.getUsername());
//      }
//
//      // Set player chat session.
//      var key = new IdentifiedKeyImpl(IdentifiedKey.Revision.LINKED_V2, packet.publicKey, packet.expiresAt, packet.keySignature);
//      player.setRemoteChatSession(new RemoteChatSession(packet.sessionId, key));
//
//
//    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
//      // TODO: do something here?
//      throw new RuntimeException(e);
//    }

    // Say we handled this packet, so the backend server does not receive it.
    return false;
  }
}