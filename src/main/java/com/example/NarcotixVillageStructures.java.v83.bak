package com.example;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.lang.reflect.Field;
import java.util.List;

public final class NarcotixVillageStructures {
    private static final int WEIGHT = 100000;

    private static final VillageStation[] STATIONS = new VillageStation[] {
            new VillageStation("village/plains/town_centers", "narcotix:village/plains/cop_station_center"),
            new VillageStation("village/desert/town_centers", "narcotix:village/desert/cop_station_center"),
            new VillageStation("village/savanna/town_centers", "narcotix:village/savanna/cop_station_center"),
            new VillageStation("village/snowy/town_centers", "narcotix:village/snowy/cop_station_center"),
            new VillageStation("village/taiga/town_centers", "narcotix:village/taiga/cop_station_center")
    };

    private static boolean registered = false;

    private NarcotixVillageStructures() {
    }

    public static void register() {
        if (registered) {
            System.out.println("[Narcotix] NarcotixVillageStructures.register() called again; ignoring duplicate.");
            return;
        }

        registered = true;
        System.out.println("[Narcotix] NarcotixVillageStructures.register() called. Using center police station injection.");

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            System.out.println("[Narcotix] SERVER_STARTING reached; injecting center cop stations into village town centers.");
            injectAll(server.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL));
        });
    }

    private static void injectAll(Registry<StructureTemplatePool> templatePoolRegistry) {
        int injected = 0;
        for (VillageStation station : STATIONS) {
            if (injectOne(templatePoolRegistry, station)) {
                injected++;
            }
        }

        System.out.println("[Narcotix] Center cop station village injection finished. Pools injected: " + injected + "/" + STATIONS.length + ".");
    }

    private static boolean injectOne(Registry<StructureTemplatePool> registry, VillageStation station) {
        ResourceKey<StructureTemplatePool> poolKey = ResourceKey.create(
                Registries.TEMPLATE_POOL,
                Identifier.withDefaultNamespace(station.poolPath)
        );

        StructureTemplatePool pool = getPool(registry, poolKey);

        if (pool == null) {
            System.out.println("[Narcotix] Could not find template pool minecraft:" + station.poolPath + ". Center cop station was not injected.");
            return false;
        }

        if (alreadyContains(pool, station.stationId)) {
            System.out.println("[Narcotix] Center cop station already present in minecraft:" + station.poolPath + ".");
            return true;
        }

        StructurePoolElement element = StructurePoolElement
                .single(station.stationId)
                .apply(StructureTemplatePool.Projection.RIGID);

        Pair<StructurePoolElement, Integer> weightedElement = Pair.of(element, WEIGHT);

        boolean added = false;
        try {
            pool.getTemplates().add(weightedElement);
            added = true;
        } catch (Throwable throwable) {
            System.out.println("[Narcotix] Direct add to templates failed for minecraft:" + station.poolPath + ": " + throwable);
        }

        added = addToBackingLists(pool, weightedElement, element, WEIGHT) || added;

        if (added) {
            System.out.println("[Narcotix] Injected center " + station.stationId + " into minecraft:" + station.poolPath + " with weight " + WEIGHT + ".");
            return true;
        }

        System.out.println("[Narcotix] Failed to mutate minecraft:" + station.poolPath + ". Center cop station was not injected.");
        return false;
    }

    private static StructureTemplatePool getPool(Registry<StructureTemplatePool> registry, ResourceKey<StructureTemplatePool> key) {
        try {
            return registry.get(key).map(reference -> reference.value()).orElse(null);
        } catch (Throwable ignored) {
        }

        try {
            return registry.getOrThrow(key).value();
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static boolean alreadyContains(StructureTemplatePool pool, String stationId) {
        try {
            for (Pair<StructurePoolElement, Integer> pair : pool.getTemplates()) {
                if (pair != null && pair.getFirst() != null && pair.getFirst().toString().contains(stationId)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean addToBackingLists(StructureTemplatePool pool, Pair<StructurePoolElement, Integer> weightedElement, StructurePoolElement element, int weight) {
        boolean added = false;
        Class<?> current = pool.getClass();

        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (!List.class.isAssignableFrom(field.getType())) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    Object value = field.get(pool);
                    if (!(value instanceof List)) {
                        continue;
                    }

                    List list = (List) value;
                    if (list.isEmpty()) {
                        continue;
                    }

                    Object first = list.get(0);
                    if (first instanceof Pair) {
                        if (!list.contains(weightedElement)) {
                            list.add(weightedElement);
                            added = true;
                        }
                    } else if (first instanceof StructurePoolElement) {
                        for (int i = 0; i < weight; i++) {
                            list.add(element);
                        }
                        added = true;
                    }
                } catch (Throwable ignored) {
                }
            }

            current = current.getSuperclass();
        }

        return added;
    }

    private static final class VillageStation {
        private final String poolPath;
        private final String stationId;

        private VillageStation(String poolPath, String stationId) {
            this.poolPath = poolPath;
            this.stationId = stationId;
        }
    }
}