package org.thingsboard.server.common.data.topology.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.asset.Asset;

@Data
@NoArgsConstructor
public class Territory extends AssetWrapper {
    public Territory(Asset asset) {
        super(asset);
    }

    public static Territory from(Asset savedAsset) {
        return new Territory(savedAsset);
    }
}
