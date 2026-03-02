/*
 * Copyright (C) 2020 Nan1t
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

package ua.nanit.limbo.server.data;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class InfoForwarding {

    private Type type;
    private byte[] secretKey;
    private List<String> tokens;

    public Type getType() {
        return type;
    }

    public byte[] getSecretKey() {
        return secretKey;
    }

    public List<String> getTokens() {
        return tokens;
    }

    public boolean hasToken(String token) {
        return tokens != null && token != null && tokens.contains(token);
    }

    public boolean isNone() {
        return type == Type.NONE;
    }

    public boolean isLegacy() {
        return type == Type.LEGACY;
    }

    public boolean isModern() {
        return type == Type.MODERN;
    }

    public boolean isBungeeGuard() {
        return type == Type.BUNGEE_GUARD;
    }

    public enum Type {
        NONE,
        LEGACY,
        MODERN,
        BUNGEE_GUARD
    }

    public static class Serializer implements TypeSerializer<InfoForwarding> {

        private final Path root;

        public Serializer(Path root) {
            this.root = root;
        }

        @Override
        public InfoForwarding deserialize(java.lang.reflect.Type type, ConfigurationNode node) throws SerializationException {
            InfoForwarding forwarding = new InfoForwarding();

            try {
                forwarding.type = Type.valueOf(node.node("type").getString("").toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new SerializationException("Undefined info forwarding type");
            }

            if (forwarding.type == Type.MODERN) {
                String secret = node.node("secret").getString("");
                if (secret.startsWith("@")) {
                    Path path = root.resolve(secret.substring(1));
                    try {
                        forwarding.secretKey = Files.readString(path, StandardCharsets.UTF_8).trim().getBytes(StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new SerializationException(node, type, "Cannot read external secret file: " + path, e);
                    }
                } else {
                    forwarding.secretKey = secret.getBytes(StandardCharsets.UTF_8);
                }
            }

            if (forwarding.type == Type.BUNGEE_GUARD) {
                ConfigurationNode tokensNode = node.node("tokens");
                if (tokensNode.isList()) {
                    forwarding.tokens = tokensNode.getList(String.class);
                } else {
                    String tokensValue = tokensNode.getString("");
                    if (tokensValue != null && tokensValue.startsWith("@")) {
                        Path path = root.resolve(tokensValue.substring(1));
                        try {
                            forwarding.tokens = Files.readAllLines(path, StandardCharsets.UTF_8);
                        } catch (IOException e) {
                            throw new SerializationException(node, type, "Cannot read external tokens file: " + path, e);
                        }
                    } else if (tokensValue != null && !tokensValue.isEmpty()) {
                        forwarding.tokens = Collections.singletonList(tokensValue);
                    }
                }
            }

            return forwarding;
        }

        @Override
        public void serialize(java.lang.reflect.Type type, @Nullable InfoForwarding obj, ConfigurationNode node) throws SerializationException {

        }
    }

}
