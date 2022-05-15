package org.thingsboard.server.controller.converters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.topology.dto.AssetWrapper;
import org.thingsboard.server.common.data.topology.dto.Segments;
import org.thingsboard.server.common.data.topology.dto.Territory;
import org.thingsboard.server.controller.AssetController;

import lombok.SneakyThrows;

@Component
public class TopologyConverter {
    @Autowired
    private AssetController assetController;

    public Territory from(Asset asset) {
        return assign(new Territory(), asset);
    }

    public AssetId assertIdFrom(AssetWrapper wrapper) {
        return AssetId.fromString(wrapper.getId());
    }

    @SneakyThrows
    public <T extends AssetWrapper> Asset from(T wrapper) {
        if (wrapper.hasId()) {
            return assetController.getAssetById(wrapper.getId());
        }

        Asset asset = new Asset();
        asset.setName(wrapper.getName());
        asset.setType(wrapper.getType());

        return asset;
    }

    public <T extends AssetWrapper> T assign(T wrapper, Asset asset) {
        wrapper.setId(asset.getId().getId().toString());
        wrapper.setName(asset.getName());
        return wrapper;
    }

    @SneakyThrows
    public <T extends AssetWrapper> T assign(Class<T> wrapperClass, Asset asset) {
        return assign(wrapperClass.getConstructor().newInstance(), asset);
    }
}
