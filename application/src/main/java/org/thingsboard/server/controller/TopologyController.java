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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.topology.dto.*;
import org.thingsboard.server.controller.converters.TopologyConverter;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.stream.Collectors;

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

    @Autowired
    private TopologyConverter converter;


    @ApiOperation(value = "Get Territory (getAssetById)",
            notes = "Fetch the Topology object based on the provided Id. " +
                    "If the user has the authority of 'Tenant Administrator', the server checks that the asset is owned by the same tenant. " +
                    "If the user has the authority of 'Customer User', the server checks that the asset is assigned to the same customer." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}", method = RequestMethod.GET)
    @ResponseBody
    public Territory getTerritoryById(
                              @ApiParam(value = TERRITORY_ID_PARAM_DESCRIPTION)
                              @PathVariable(TERRITORY_ID) String strTerritoryId) throws ThingsboardException {
        return getById(Territory.class, strTerritoryId);
    }


    @ApiOperation(value = "Create Or Update Territory",
            notes = "Creates or Updates the Territory. When creating it proxies request and creates asset, platform generates Asset Id as " + UUID_WIKI_LINK +
                    "The newly created Asset id will be present in the response. " +
                    "Specify existing Asset id to update the asset. " +
                    "Referencing non-existing Asset Id will cause 'Not Found' error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory", method = RequestMethod.POST)
    @ResponseBody
    public Territory saveTerritory(
            @ApiParam(value = "A JSON value representing the territory.") @RequestBody Territory territory) throws ThingsboardException {
        Asset postedAsset = converter.from(territory);
        Asset savedAsset = assetController.saveAsset(postedAsset);
        return converter.assign(territory.getClass(), savedAsset);
    }

    @ApiOperation(value = "Create Or Update Buildings",
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

        return save(building, strTerritoryId);
    }

    @ApiOperation(value = "Get Territory",
            notes = "Fetch the Territory object based on the provided Asset Id. " +
                    "If the user has the authority of 'Tenant Administrator', the server checks that the asset is owned by the same tenant. " +
                    "If the user has the authority of 'Customer User', the server checks that the asset is assigned to the same customer." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}/building/{buildingId}", method = RequestMethod.GET)
    @ResponseBody
    public Building getBuildingById(@ApiParam(value = ASSET_ID_PARAM_DESCRIPTION)
                                          @PathVariable(TERRITORY_ID) String strTerritoryId,
                                    @PathVariable(BUILDING_ID) String strAssetId) throws ThingsboardException {
        return getById(Building.class, strAssetId);
    }

    @ApiOperation(value = "Find related buildings",
            notes = "Returns all assets that are related to the specific entity. " +
                    "The entity id, relation type, asset types, depth of the search, and other query parameters defined using complex 'AssetSearchQuery' object. " +
                    "See 'Model' tab of the Parameters for more info.", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}/buildings", method = RequestMethod.GET)
    @ResponseBody
    public List<Building> findBuildingsByQuery(
            @PathVariable(TERRITORY_ID) String strTerritoryId) throws ThingsboardException {
        return findAllItems(Building.class, Segments.BUILDING, strTerritoryId);
    }

    @SneakyThrows
    protected <T extends AssetWrapper> List<T> findAllItems(Class<T> targetClass, Segments type, String parentId) throws ThingsboardException {
        RelationsSearchParameters relationParameters = new RelationsSearchParameters(
                EntityIdFactory.getByTypeAndUuid(EntityType.ASSET, parentId),
                EntitySearchDirection.FROM,
                1,
                false
        );

        AssetSearchQuery searchQuery = new AssetSearchQuery();
        searchQuery.setAssetTypes(List.of(type.getKey()));
        searchQuery.setRelationType(RELATION_TYPE_CONTAINS);
        searchQuery.setParameters(relationParameters);

        Constructor<T> constructor = targetClass.getConstructor();
        return assetController.findByQuery(searchQuery).stream().map(
                a -> {
                    try {
                        return converter.assign(constructor.newInstance(), a);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
            .collect(Collectors.toList());
    }

    @ApiOperation(value = "Create Or Update Room",
            notes = "Creates or Updates the Asset. When creating room, platform generates Asset Id as " + UUID_WIKI_LINK +
                    "The newly created Asset id will be present in the response. " +
                    "Specify existing Asset id to update the room. " +
                    "Referencing non-existing Asset Id will cause 'Not Found' error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}/building/{buildingId}/room", method = RequestMethod.POST)
    @ResponseBody
    public Room saveRoomAsset(@ApiParam(value = "A JSON value representing the room.")
                              @PathVariable(TERRITORY_ID) String strTerritoryId,
                              @PathVariable(BUILDING_ID) String strBuildingId,
                              @RequestBody Room room) throws ThingsboardException {
        return save(room, strBuildingId);
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
        EntityRelation entityRelation = new EntityRelation();
        entityRelation.setFrom(AssetId.fromString(strRoomId));
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

    @ApiOperation(value = "Find related buildings",
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


        return findAllItems(Room.class, Segments.ROOM, strBuildingId);
    }

    @ApiOperation(value = "Get Territory",
            notes = "Fetch the Territory object based on the provided Asset Id. " +
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
        return getById(Room.class, strRoomId);
    }


    @SneakyThrows
    protected <T extends AssetWrapper> T getById(Class<T> targetClass, String id) throws ThingsboardException {
        Asset asset = assetController.getAssetById(id);

        return converter.assign(targetClass.getConstructor().newInstance(), asset);
    }

    protected <T extends AssetWrapper> T save(T posted, String parentId) throws ThingsboardException {
        Asset postedAsset = converter.from(posted);
        Asset savedAsset = assetController.saveAsset(postedAsset);
        Class<T> classRef = (Class<T>) posted.getClass(); //fix cast
        T saved = converter.assign(classRef, savedAsset);

        EntityRelation entityRelation = new EntityRelation();
        entityRelation.setFrom(AssetId.fromString(parentId));
        entityRelation.setType(RELATION_TYPE_CONTAINS);
        entityRelation.setTo(AssetId.fromString(saved.getId()));

        relationController.saveRelation(entityRelation);

        return saved;
    }


}
