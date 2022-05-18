/**
 * Copyright © 2016-2022 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller.converters;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.topology.dto.BaseWrapper;
import org.thingsboard.server.common.data.topology.dto.Territory;
import org.thingsboard.server.common.data.topology.dto.TopologyDevice;
import org.thingsboard.server.controller.AssetController;
import org.thingsboard.server.controller.DeviceController;

import java.util.function.Function;

@Component
public class TopologyConverter {
    @Autowired
    private AssetController assetController;

    @Autowired
    private DeviceController deviceController;

    public Territory from(Asset asset) {
        return assign(new Territory(), asset);
    }

    @SneakyThrows
    public <T extends BaseWrapper> Asset from(T wrapper) {
        Asset asset;

        if (wrapper.hasId()) {
            asset = assetController.getAssetById(wrapper.getId());
        } else {
            asset = new Asset();
        }

        asset.setName(wrapper.getName());
        asset.setType(wrapper.getType());

        return asset;
    }

    public <T extends BaseWrapper> T assign(T wrapper, Asset asset) {
        wrapper.setId(asset.getId().getId().toString());
        wrapper.setName(asset.getName());
        return wrapper;
    }

    @SneakyThrows
    public <T extends BaseWrapper> T assign(Class<T> wrapperClass, Asset asset) {
        return assign(wrapperClass.getConstructor().newInstance(), asset);
    }

    @SneakyThrows
    public TopologyDevice from(Device device) {
        TopologyDevice wrapper = new TopologyDevice();

        wrapper.setId(device.getId().getId().toString());
        wrapper.setName(device.getName());

        return wrapper;
    }

    @SneakyThrows
    public Device from(TopologyDevice wrapper) {
        Device device;

        if (wrapper.hasId()) {
            device = deviceController.getDeviceById(wrapper.getId());
        } else {
            device = new Device();
        }

        device.setName(wrapper.getName());
        device.setType(wrapper.getType());

        return device;
    }

    public <T extends BaseWrapper> PageData<T> toPage(Class<T> targetClass, PageData<Asset> assetPage) {
        return assetPage.mapData(new Function<Asset, T>() {
            @Override
            public T apply(Asset asset) {
                return assign(targetClass, asset);
            }
        });
    }

    public PageData<TopologyDevice> toPage(PageData<Device> assetPage) {
        return assetPage.mapData(new Function<Device, TopologyDevice>() {
            @Override
            public TopologyDevice apply(Device device) {
                return from(device);
            }
        });
    }
}
