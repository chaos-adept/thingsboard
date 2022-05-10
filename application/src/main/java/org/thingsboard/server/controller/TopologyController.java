/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.topology.dto.Building;
import org.thingsboard.server.common.data.topology.dto.Segments;
import org.thingsboard.server.common.data.topology.dto.Territory;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.thingsboard.server.controller.AssetController.ASSET_ID;
import static org.thingsboard.server.controller.ControllerConstants.*;

@RestController
@TbCoreComponent
@RequestMapping("/api/topology")
@RequiredArgsConstructor
@Slf4j
public class TopologyController extends BaseController {

    public static final String TERRITORY_ID = "territoryId";

    @Autowired
    private AssetController assetController;

    @Autowired
    private EntityRelationController relationController;

    @ApiOperation(value = "Get Topology (getAssetById)",
            notes = "Fetch the Topology object based on the provided Asset Id. " +
                    "If the user has the authority of 'Tenant Administrator', the server checks that the asset is owned by the same tenant. " +
                    "If the user has the authority of 'Customer User', the server checks that the asset is assigned to the same customer." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{assetId}", method = RequestMethod.GET)
    @ResponseBody
    public Territory getAssetById(@ApiParam(value = ASSET_ID_PARAM_DESCRIPTION)
                              @PathVariable(ASSET_ID) String strAssetId) throws ThingsboardException {
        return Territory.from(assetController.getAssetById(strAssetId));
    }


    @ApiOperation(value = "Get Territory (getAssetById)",
            notes = "Fetch the Topology object based on the provided Asset Id. " +
                    "If the user has the authority of 'Tenant Administrator', the server checks that the asset is owned by the same tenant. " +
                    "If the user has the authority of 'Customer User', the server checks that the asset is assigned to the same customer." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{assetId}", method = RequestMethod.GET)
    @ResponseBody
    public Territory getTerritoryById(@ApiParam(value = ASSET_ID_PARAM_DESCRIPTION)
                              @PathVariable(ASSET_ID) String strAssetId) throws ThingsboardException {
        return Territory.from(assetController.getAssetById(strAssetId));
    }


    @ApiOperation(value = "Create Or Update Territory (saveAsset)",
            notes = "Creates or Updates the Asset. When creating asset, platform generates Asset Id as " + UUID_WIKI_LINK +
                    "The newly created Asset id will be present in the response. " +
                    "Specify existing Asset id to update the asset. " +
                    "Referencing non-existing Asset Id will cause 'Not Found' error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory", method = RequestMethod.POST)
    @ResponseBody
    public Territory saveAsset(@ApiParam(value = "A JSON value representing the asset.") @RequestBody Territory asset) throws ThingsboardException {
        checkAndSetType(asset.getAsset(), Segments.TERRITORY);
        //todo log data correctly according to the architecture
        return Territory.from(assetController.saveAsset(asset.getAsset()));
    }


    @ApiOperation(value = "Create Or Update Buldings (saveAsset)",
            notes = "Creates or Updates the Asset. When creating asset, platform generates Asset Id as " + UUID_WIKI_LINK +
                    "The newly created Asset id will be present in the response. " +
                    "Specify existing Asset id to update the asset. " +
                    "Referencing non-existing Asset Id will cause 'Not Found' error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}/building", method = RequestMethod.POST)
    @ResponseBody
    public Building saveBuildingAsset(@ApiParam(value = "A JSON value representing the asset.")
                                      @PathVariable(TERRITORY_ID) String strTerritoryId,
                                      @RequestBody Building asset) throws ThingsboardException {
        checkAndSetType(asset.getAsset(), Segments.BUILDING);
        //todo log data correctly according to the architecture
        Territory territory = getAssetById(strTerritoryId);


        Building savedBuilding = new Building(assetController.saveAsset(asset.getAsset()));

        //todo make a relation
        EntityRelation entityRelation = new EntityRelation();
        entityRelation.setFrom(territory.getAsset().getId());
        entityRelation.setType("Contains"); //todo extract to constants
        entityRelation.setTo(savedBuilding.getAsset().getId());

        relationController.saveRelation(entityRelation);

        return savedBuilding;
    }

    @ApiOperation(value = "Get Territory (getAssetById)",
            notes = "Fetch the Topology object based on the provided Asset Id. " +
                    "If the user has the authority of 'Tenant Administrator', the server checks that the asset is owned by the same tenant. " +
                    "If the user has the authority of 'Customer User', the server checks that the asset is assigned to the same customer." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}/building/{assetId}", method = RequestMethod.GET)
    @ResponseBody
    public Building getBuildingById(@ApiParam(value = ASSET_ID_PARAM_DESCRIPTION)
                                          @PathVariable(TERRITORY_ID) String strTerritoryId,
                                    @PathVariable(ASSET_ID) String strAssetId) throws ThingsboardException {
        return new Building(assetController.getAssetById(strAssetId));
    }

    @ApiOperation(value = "Find related buildings (findByQuery)",
            notes = "Returns all assets that are related to the specific entity. " +
                    "The entity id, relation type, asset types, depth of the search, and other query parameters defined using complex 'AssetSearchQuery' object. " +
                    "See 'Model' tab of the Parameters for more info.", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}/buildings", method = RequestMethod.GET)
    @ResponseBody
    public List<Building> findBuildingsByQuery(
            @PathVariable(TERRITORY_ID) String strTerritoryId) throws ThingsboardException {


        RelationsSearchParameters relationParameters = new RelationsSearchParameters(
                EntityIdFactory.getByTypeAndUuid(EntityType.ASSET, strTerritoryId),
                EntitySearchDirection.FROM,
                1,
                false
        );

        AssetSearchQuery searchQuery = new AssetSearchQuery();
        searchQuery.setAssetTypes(List.of(Segments.BUILDING.getKey()));
        searchQuery.setRelationType("Contains");
        searchQuery.setParameters(relationParameters);

        return assetController.findByQuery(searchQuery).stream().map(Building::new).collect(Collectors.toList());
    }


    private void checkAndSetType(Asset asset, Segments segment) throws ThingsboardException {
        var definedType = asset.getType();
        if (Objects.nonNull(definedType) && !segment.getKey().equals(definedType)) {
            throw new ThingsboardException(
                    "Unexpected asset type " + definedType, ThingsboardErrorCode.INVALID_ARGUMENTS);
        }

        asset.setType(segment.getKey());
    }


}
