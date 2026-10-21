package dev.elbgg.capcams.narrative;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class NarrativeGuard {
    private static final MethodHandle BEGIN;
    private static final MethodHandle END;

    static {
        MethodHandle begin = null, end = null;
        try {
            Class<?> api = Class.forName("fr.loudo.narrativecraft.api.client.NarrativeCraftClientAPI");
            var lookup = MethodHandles.publicLookup();
            begin = lookup.findStatic(api, "beginSecondaryView", MethodType.methodType(void.class));
            end = lookup.findStatic(api, "endSecondaryView", MethodType.methodType(void.class));
        } catch (Throwable ignored) {
        }
        BEGIN = begin;
        END = end;
    }

    private NarrativeGuard() {}

    public static void begin() {
        invoke(BEGIN);
    }

    public static void end() {
        invoke(END);
    }

    private static void invoke(MethodHandle handle) {
        if (handle == null) return;
        try {
            handle.invokeExact();
        } catch (Throwable ignored) {
        }
    }
}
