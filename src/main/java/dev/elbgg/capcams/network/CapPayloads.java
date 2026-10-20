package dev.elbgg.capcams.network;

import dev.elbgg.capcams.CapCams;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class CapPayloads {
    private CapPayloads() {}

    private static <T extends CustomPayload> CustomPayload.Id<T> pid(String path) {
        return new CustomPayload.Id<>(Identifier.of(CapCams.MOD_ID, path));
    }

    public record Split(String layout, float seconds) implements CustomPayload {
        public static final Id<Split> ID = pid("split");
        public static final PacketCodec<RegistryByteBuf, Split> CODEC = CustomPayload.codecOf(
                (v, b) -> {
                    b.writeString(v.layout, 512);
                    b.writeFloat(v.seconds);
                },
                b -> new Split(b.readString(512), b.readFloat()));

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record Panel(int index, String source) implements CustomPayload {
        public static final Id<Panel> ID = pid("panel");
        public static final PacketCodec<RegistryByteBuf, Panel> CODEC = CustomPayload.codecOf(
                (v, b) -> {
                    b.writeVarInt(v.index);
                    b.writeString(v.source, 512);
                },
                b -> new Panel(b.readVarInt(), b.readString(512)));

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record Clear(float seconds) implements CustomPayload {
        public static final Id<Clear> ID = pid("clear");
        public static final PacketCodec<RegistryByteBuf, Clear> CODEC = CustomPayload.codecOf(
                (v, b) -> b.writeFloat(v.seconds), b -> new Clear(b.readFloat()));

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record Sync(String shots, String scenes) implements CustomPayload {
        public static final Id<Sync> ID = pid("sync");
        public static final PacketCodec<RegistryByteBuf, Sync> CODEC = CustomPayload.codecOf(
                (v, b) -> {
                    b.writeString(v.shots, 4_000_000);
                    b.writeString(v.scenes, 4_000_000);
                },
                b -> new Sync(b.readString(4_000_000), b.readString(4_000_000)));

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record Notice(String message) implements CustomPayload {
        public static final Id<Notice> ID = pid("notice");
        public static final PacketCodec<RegistryByteBuf, Notice> CODEC = CustomPayload.codecOf(
                (v, b) -> b.writeString(v.message, 1024), b -> new Notice(b.readString(1024)));

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record Edit(String op, String name, String arg, String json) implements CustomPayload {
        public static final Id<Edit> ID = pid("edit");
        public static final PacketCodec<RegistryByteBuf, Edit> CODEC = CustomPayload.codecOf(
                (v, b) -> {
                    b.writeString(v.op, 32);
                    b.writeString(v.name, 128);
                    b.writeString(v.arg, 128);
                    b.writeString(v.json, 200_000);
                },
                b -> new Edit(b.readString(32), b.readString(128), b.readString(128), b.readString(200_000)));

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
