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
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.topology.dto.Building;
import org.thingsboard.server.common.data.topology.dto.DeviceAssigment;
import org.thingsboard.server.common.data.topology.dto.Room;
import org.thingsboard.server.common.data.topology.dto.Segments;
import org.thingsboard.server.common.data.topology.dto.Territory;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.AssetController.ASSET_ID;
import static org.thingsboard.server.controller.ControllerConstants.*;

@RestController
@TbCoreComponent
@RequestMapping("/api/topology")
@RequiredArgsConstructor
@Slf4j
public class TopologyController extends BaseController {

    public static final String TERRITORY_ID = "territoryId";
    public static final String BUILDING_ID = "buildingId";
    public static final String ROOM_ID = "roomId";
    public static final String RELATION_TYPE_CONTAINS = "Contains";

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
    @RequestMapping(value = "/territory/{territoryId}", method = RequestMethod.GET)
    @ResponseBody
    public Territory getTerritoryById(@ApiParam(value = ASSET_ID_PARAM_DESCRIPTION)
                              @PathVariable(TERRITORY_ID) String strAssetId) throws ThingsboardException {
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
            notes = "Creates or Updates the Asset. When creating building, platform generates Asset Id as " + UUID_WIKI_LINK +
                    "The newly created Asset id will be present in the response. " +
                    "Specify existing Asset id to update the building. " +
                    "Referencing non-existing Asset Id will cause 'Not Found' error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}/building", method = RequestMethod.POST)
    @ResponseBody
    public Building saveBuildingAsset(@ApiParam(value = "A JSON value representing the building.")
                                      @PathVariable(TERRITORY_ID) String strTerritoryId,
                                      @RequestBody Building building) throws ThingsboardException {
        checkAndSetType(building.getAsset(), Segments.BUILDING);
        //todo log data correctly according to the architecture
        Territory territory = getAssetById(strTerritoryId);


        Building savedBuilding = new Building(assetController.saveAsset(building.getAsset()));

        EntityRelation entityRelation = new EntityRelation();
        entityRelation.setFrom(territory.getAsset().getId());
        entityRelation.setType(RELATION_TYPE_CONTAINS);
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
    @RequestMapping(value = "/territory/{territoryId}/building/{buildingId}", method = RequestMethod.GET)
    @ResponseBody
    public Building getBuildingById(@ApiParam(value = ASSET_ID_PARAM_DESCRIPTION)
                                          @PathVariable(TERRITORY_ID) String strTerritoryId,
                                    @PathVariable(BUILDING_ID) String strAssetId) throws ThingsboardException {
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
        searchQuery.setRelationType(RELATION_TYPE_CONTAINS);
        searchQuery.setParameters(relationParameters);

        return assetController.findByQuery(searchQuery).stream().map(Building::new).collect(Collectors.toList());
    }

    @ApiOperation(value = "Create Or Update Room (saveAsset)",
            notes = "Creates or Updates the Asset. When creating asset, platform generates Asset Id as " + UUID_WIKI_LINK +
                    "The newly created Asset id will be present in the response. " +
                    "Specify existing Asset id to update the asset. " +
                    "Referencing non-existing Asset Id will cause 'Not Found' error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}/building/{buildingId}/room", method = RequestMethod.POST)
    @ResponseBody
    public Room saveRoomAsset(@ApiParam(value = "A JSON value representing the asset.")
                              @PathVariable(TERRITORY_ID) String strTerritoryId,
                              @PathVariable(BUILDING_ID) String strBuildingId,
                              @RequestBody Room asset) throws ThingsboardException {
        checkAndSetType(asset.getAsset(), Segments.ROOM);
        //todo log data correctly according to the architecture
        Building building = new Building(assetController.getAssetById(strBuildingId));

        //todo verify that building belogs to territory

        Room savedRoom = new Room(assetController.saveAsset(asset.getAsset()));

        EntityRelation entityRelation = new EntityRelation();
        entityRelation.setFrom(building.getAsset().getId());
        entityRelation.setType(RELATION_TYPE_CONTAINS);
        entityRelation.setTo(savedRoom.getAsset().getId());

        relationController.saveRelation(entityRelation);

        return savedRoom;
    }

    @ApiOperation(value = "Create Or Update Room (saveAsset)",
            notes = "Creates or Updates the Asset. When creating assigment, platform generates Asset Id as " + UUID_WIKI_LINK +
                    "The newly created Asset id will be present in the response. " +
                    "Specify existing Asset id to update the assigment. " +
                    "Referencing non-existing Asset Id will cause 'Not Found' error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}/building/{buildingId}/room/{roomId}/deviceAssigment", method = RequestMethod.POST)
    @ResponseBody
    public DeviceAssigment saveRoomAsset(@ApiParam(value = "A JSON value representing the assigment.")
                              @PathVariable(TERRITORY_ID) String strTerritoryId,
                              @PathVariable(BUILDING_ID) String strBuildingId,
                              @PathVariable(ROOM_ID) String strRoomId,
                              @RequestBody DeviceAssigment assigment) throws ThingsboardException {
        //todo log data correctly according to the architecture
        Room room = new Room(assetController.getAssetById(strRoomId));

        //todo verify that room belongs to territory

        EntityRelation entityRelation = new EntityRelation();
        entityRelation.setFrom(room.getAsset().getId());
        entityRelation.setType(RELATION_TYPE_CONTAINS);
        entityRelation.setTo(EntityIdFactory.getByTypeAndUuid(EntityType.DEVICE, assigment.getDeviceId()));

        relationController.saveRelation(entityRelation);

        return assigment;
    }

    @ApiOperation(value = "Find related device assigments (findByQuery)",
            notes = "Returns all assets that are related to the specific entity. " +
                    "The entity id, relation type, asset types, depth of the search, and other query parameters defined using complex 'AssetSearchQuery' object. " +
                    "See 'Model' tab of the Parameters for more info.", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}/building/{buildingId}/room/{roomId}/deviceAssignments", method = RequestMethod.GET)
    @ResponseBody
    public List<DeviceAssigment> findDevicesByQuery(
            @PathVariable(TERRITORY_ID) String strTerritoryId,
            @PathVariable(BUILDING_ID) String strBuildingId,
            @PathVariable(ROOM_ID) String strRoomId
    ) throws ThingsboardException {
        RelationsSearchParameters relationParameters = new RelationsSearchParameters(
                EntityIdFactory.getByTypeAndUuid(EntityType.ASSET, strRoomId),
                EntitySearchDirection.FROM,
                1,
                false
        );

        EntityRelationsQuery searchQuery = new EntityRelationsQuery();
        searchQuery.setFilters(List.of(
                new RelationEntityTypeFilter(RELATION_TYPE_CONTAINS, List.of(EntityType.DEVICE))));
        searchQuery.setParameters(relationParameters);

        return relationController.findByQuery(searchQuery).stream()
                .map(r -> new DeviceAssigment(r.getTo().getId().toString()))
                .collect(Collectors.toList());
    }

    @ApiOperation(value = "Find related buildings (findByQuery)",
            notes = "Returns all assets that are related to the specific entity. " +
                    "The entity id, relation type, asset types, depth of the search, and other query parameters defined using complex 'AssetSearchQuery' object. " +
                    "See 'Model' tab of the Parameters for more info.", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}/building/{buildingId}/rooms", method = RequestMethod.GET)
    @ResponseBody
    public List<Room> findRoomsByQuery(
            @PathVariable(TERRITORY_ID) String strTerritoryId,
            @PathVariable(BUILDING_ID) String strBuildingId
            ) throws ThingsboardException {


        RelationsSearchParameters relationParameters = new RelationsSearchParameters(
                EntityIdFactory.getByTypeAndUuid(EntityType.ASSET, strTerritoryId),
                EntitySearchDirection.FROM,
                1,
                false
        );

        AssetSearchQuery searchQuery = new AssetSearchQuery();
        searchQuery.setAssetTypes(List.of(Segments.ROOM.getKey()));
        searchQuery.setRelationType(RELATION_TYPE_CONTAINS);
        searchQuery.setParameters(relationParameters);

        return assetController.findByQuery(searchQuery).stream().map(Room::new).collect(Collectors.toList());
    }

    @ApiOperation(value = "Get Territory (getAssetById)",
            notes = "Fetch the Topology object based on the provided Asset Id. " +
                    "If the user has the authority of 'Tenant Administrator', the server checks that the asset is owned by the same tenant. " +
                    "If the user has the authority of 'Customer User', the server checks that the asset is assigned to the same customer." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}/building/{buildingId}/room/{roomId}", method = RequestMethod.GET)
    @ResponseBody
    public Room getBuildingById(@ApiParam(value = ASSET_ID_PARAM_DESCRIPTION)
                                    @PathVariable(TERRITORY_ID) String strTerritoryId,
                                    @PathVariable(BUILDING_ID) String strBuildingId,
                                    @PathVariable(ROOM_ID) String strRoomId) throws ThingsboardException {
        return new Room(assetController.getAssetById(strRoomId));
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
