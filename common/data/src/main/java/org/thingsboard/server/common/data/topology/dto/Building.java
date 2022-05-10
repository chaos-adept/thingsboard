package org.thingsboard.server.common.data.topology.dto;

import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.asset.Asset;

@NoArgsConstructor
public class Building extends AssetWrapper {

    public Building(Asset asset) {
        super(asset);
    }
}
