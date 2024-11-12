/*
 * Copyright (C) 2020-2023 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet.chat;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.chat.session.PlayerSessionPacket;

public interface ChatHandler<T extends MinecraftPacket> {

  Class<T> packetClass();

  void handlePlayerChatInternal(T packet);

  default boolean handlePlayerChat(MinecraftPacket packet) {
    if (packetClass().isInstance(packet)) {
      handlePlayerChatInternal(packetClass().cast(packet));
      return true;
    }
    return false;
  }

  default boolean handlePlayerSession(PlayerSessionPacket packet) {
    return false;
  }
}
