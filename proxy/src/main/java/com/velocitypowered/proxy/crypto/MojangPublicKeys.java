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

package com.velocitypowered.proxy.crypto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.util.ProxyVersion;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MojangPublicKeys {
  private static final Logger logger = LogManager.getLogger(MojangPublicKeys.class);

  final List<PublicKey> profilePropertyKeys;
  final List<PublicKey> playerCertificateKeys;

  public MojangPublicKeys(ProxyVersion version) {
    final String url = "https://api.minecraftservices.com/publickeys";
    final HttpRequest httpRequest = HttpRequest.newBuilder()
        .setHeader("User-Agent",
            version.getName() + "/" + version.getVersion())
        .uri(URI.create(url))
        .build();

    final HttpClient httpClient = HttpClient.newHttpClient();

    try {
      var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      logger.info(response.body());
      JsonObject object = JsonParser.parseString(response.body()).getAsJsonObject();

      profilePropertyKeys = getKeysFromJson(object.get("profilePropertyKeys"));
      playerCertificateKeys = getKeysFromJson(object.get("playerCertificateKeys"));
    } catch (IOException | InterruptedException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }

  public List<PublicKey> getProfilePropertyKeys() {
    return profilePropertyKeys;
  }

  public List<PublicKey> getPlayerCertificateKeys() {
    return playerCertificateKeys;
  }

  private List<PublicKey> getKeysFromJson(JsonElement array) throws NoSuchAlgorithmException, InvalidKeySpecException {
    var publicKeys = new ArrayList<PublicKey>();
    for (JsonElement jsonElement : array.getAsJsonArray()) {
      String keyString = jsonElement.getAsJsonObject().get("publicKey").getAsString();
      byte[] keyBase64 = Base64.getDecoder().decode(keyString);
      EncodedKeySpec KeyX509 = new X509EncodedKeySpec(keyBase64);

      KeyFactory kf = KeyFactory.getInstance("RSA");
      var key = kf.generatePublic(KeyX509);
      publicKeys.add(key);
    }

    return publicKeys;
  }
}
