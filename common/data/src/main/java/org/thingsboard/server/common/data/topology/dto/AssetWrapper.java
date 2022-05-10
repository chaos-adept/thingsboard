package org.thingsboard.server.common.data.topology.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.asset.Asset;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssetWrapper {

    private Asset asset;

}
