/*
 * This file is part of ImageFrame.
 *
 * Copyright (C) 2022. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2022. Contributors
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.imageframe.objectholders;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.loohp.imageframe.utils.MapUtils;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class ImageMap {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static ImageMap load(ImageMapManager manager, File folder) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(new File(folder, "data.json").toPath()), StandardCharsets.UTF_8))) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            String type = json.get("type").getAsString();
            return (ImageMap) Class.forName(type).getMethod("load", ImageMapManager.class, File.class, JsonObject.class).invoke(null, manager, folder, json);
        }
    }

    protected final ImageMapManager manager;

    protected int imageIndex;
    protected final List<MapView> mapViews;
    protected final IntList mapIds;
    protected final int width;
    protected final int height;
    protected final UUID creator;
    protected final long creationTime;

    public ImageMap(ImageMapManager manager, int imageIndex, List<MapView> mapViews, IntList mapIds, int width, int height, UUID creator, long creationTime) {
        if (mapViews.size() != width * height) {
            throw new IllegalArgumentException("mapViews size does not equal width * height");
        }
        this.manager = manager;
        this.imageIndex = imageIndex;
        this.mapViews = Collections.unmodifiableList(mapViews);
        this.mapIds = IntLists.unmodifiable(mapIds);
        this.width = width;
        this.height = height;
        this.creator = creator;
        this.creationTime = creationTime;
    }

    public ImageMapManager getManager() {
        return manager;
    }

    public int getImageIndex() {
        return imageIndex;
    }

    public void stop() {
        //do nothing
    }

    public boolean requiresAnimationService() {
        return false;
    }

    public byte[] getRawAnimationColors(int currentTick, int index) {
        throw new UnsupportedOperationException("this map does not requires animation");
    }

    public abstract void update() throws Exception;

    public void send(Collection<? extends Player> players) {
        for (MapView mapView : mapViews) {
            players.forEach(p -> p.sendMap(mapView));
        }
    }

    public abstract void save(File dataFolder) throws Exception;

    public void giveMaps(Collection<? extends Player> players) {
        for (MapView mapView : mapViews) {
            ItemStack itemStack = new ItemStack(Material.FILLED_MAP);
            MapMeta mapMeta = (MapMeta) itemStack.getItemMeta();
            mapMeta.setMapView(mapView);
            itemStack.setItemMeta(mapMeta);
            players.forEach(p -> {
                HashMap<Integer, ItemStack> result = p.getInventory().addItem(itemStack.clone());
                for (ItemStack stack : result.values()) {
                    p.getWorld().dropItem(p.getEyeLocation(), stack);
                }
            });
        }
    }

    public Set<Player> getViewers() {
        Set<Player> players = new HashSet<>();
        for (MapView mapView : mapViews) {
            Set<Player> set = MapUtils.getViewers(mapView);
            if (set != null) {
                players.addAll(set);
            }
        }
        return players;
    }

    public UUID getCreator() {
        return creator;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public List<MapView> getMapViews() {
        return mapViews;
    }

    public MapView getMapViewFromMapId(int mapId) {
        return mapViews.get(mapIds.indexOf(mapId));
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}