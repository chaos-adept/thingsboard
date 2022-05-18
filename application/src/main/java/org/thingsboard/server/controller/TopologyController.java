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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.topology.NarrowAssetSearchQuery;
import org.thingsboard.server.common.data.topology.NarrowDeviceSearchQuery;
import org.thingsboard.server.common.data.topology.TopologyLevel;
import org.thingsboard.server.common.data.topology.dto.*;
import org.thingsboard.server.controller.converters.TopologyConverter;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
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
    public static final String RELATION_TYPE_CONTAINS = EntityRelation.CONTAINS_TYPE;

    @Autowired
    private AssetController assetController;

    @Autowired
    private EntityRelationController relationController;

    @Autowired
    private DeviceController deviceController;

    @Autowired
    private TopologyConverter converter;

    @Autowired
    private AssetService assetService;

    @ApiOperation(value = "Get Territories",
            notes = "Returns all territories in the tenant", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territories", method = RequestMethod.GET)
    @ResponseBody
    public PageData<Territory> findTerritoriesByQuery(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @ApiParam(value = ASSET_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ASSET_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        return getPageData(
                null, Territory.class, TopologyLevel.TERRITORY,
                pageSize, page, textSearch, sortProperty, sortOrder);
    }

    @ApiOperation(value = "Get Territory",
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

    @ApiOperation(value = "Delete Territory",
            notes = "Deletes the Territory and all the relations (from and to the asset). Referencing non-existing asset Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/territory/{territoryId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteTerritory(
            @ApiParam(value = TERRITORY_ID_PARAM_DESCRIPTION) @PathVariable(TERRITORY_ID) String strTerritoryId) throws ThingsboardException {
        deleteGeneric(strTerritoryId);
    }

    @ApiOperation(value = "Create Or Update Buildings",
            notes = "Creates or Updates the Asset. When creating building, platform generates Asset Id as " + UUID_WIKI_LINK +
                    "The newly created Asset id will be present in the response. " +
                    "Specify existing Asset id to update the building. " +
                    "Referencing non-existing Asset Id will cause 'Not Found' error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}/building", method = RequestMethod.POST)
    @ResponseBody
    public Building saveBuildingAsset(
            @ApiParam(value = TERRITORY_ID_PARAM_DESCRIPTION) @PathVariable(TERRITORY_ID) String strTerritoryId,
            @ApiParam(value = "A JSON value representing the building.") @RequestBody Building building) throws ThingsboardException {
        return save(building, strTerritoryId);
    }

    @ApiOperation(value = "Delete Buildings",
            notes = "Deletes the Building and all the relations (from and to the asset). Referencing non-existing asset Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/territory/{territoryId}/building/{buildingId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteBuilding(
            @ApiParam(value = TERRITORY_ID_PARAM_DESCRIPTION) @PathVariable(TERRITORY_ID) String strTerritoryId,
            @ApiParam(value = BUILDING_ID_PARAM_DESCRIPTION) @PathVariable(BUILDING_ID) String strBuildingId) throws ThingsboardException {
        checkRelations(List.of(
                assetId(strTerritoryId),
                assetId(strBuildingId)
        ));

        deleteGeneric(strBuildingId);
    }

    @ApiOperation(value = "Get Building",
            notes = "Fetch the Building object based on the provided Asset Id. " +
                    "If the user has the authority of 'Tenant Administrator', the server checks that the asset is owned by the same tenant. " +
                    "If the user has the authority of 'Customer User', the server checks that the asset is assigned to the same customer." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}/building/{buildingId}", method = RequestMethod.GET)
    @ResponseBody
    public Building getBuildingById(
            @ApiParam(value = TERRITORY_ID_PARAM_DESCRIPTION)
            @PathVariable(TERRITORY_ID) String strTerritoryId,

            @ApiParam(value = BUILDING_ID_PARAM_DESCRIPTION)
            @PathVariable(BUILDING_ID) String strBuildingId) throws ThingsboardException {
        checkRelations(List.of(
                assetId(strTerritoryId),
                assetId(strBuildingId)
        ));
        return getById(Building.class, strBuildingId);
    }

    @ApiOperation(value = "Get related Buildings",
            notes = "Returns all assets that are related to the specific entity. ", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}/buildings", method = RequestMethod.GET)
    @ResponseBody
    public PageData<Building> findBuildingsByQuery(
            @ApiParam(value = TERRITORY_ID_PARAM_DESCRIPTION)
            @PathVariable(TERRITORY_ID) String strTerritoryId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @ApiParam(value = ASSET_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ASSET_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder
            ) throws ThingsboardException {
        return getPageData(strTerritoryId, Building.class, TopologyLevel.BUILDING, pageSize, page, textSearch, sortProperty, sortOrder);
    }

    @ApiOperation(value = "Create Or Update Room",
            notes = "Creates or Updates the Asset. When creating room, platform generates Asset Id as " + UUID_WIKI_LINK +
                    "The newly created Asset id will be present in the response. " +
                    "Specify existing Asset id to update the room. " +
                    "Referencing non-existing Asset Id will cause 'Not Found' error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}/building/{buildingId}/room", method = RequestMethod.POST)
    @ResponseBody
    public Room saveRoom(
           @ApiParam(value = TERRITORY_ID_PARAM_DESCRIPTION)
           @PathVariable(TERRITORY_ID) String strTerritoryId,
           @ApiParam(value = BUILDING_ID_PARAM_DESCRIPTION)
           @PathVariable(BUILDING_ID) String strBuildingId,
           @ApiParam(value = "A JSON value representing the room.") @RequestBody Room room) throws ThingsboardException {
        checkRelations(List.of(
                assetId(strTerritoryId),
                assetId(strBuildingId)
        ));

        return save(room, strBuildingId);
    }

    @ApiOperation(value = "Get Room",
            notes = "Fetch the Room object based on the provided Asset Id. " +
                    "If the user has the authority of 'Tenant Administrator', the server checks that the asset is owned by the same tenant. " +
                    "If the user has the authority of 'Customer User', the server checks that the asset is assigned to the same customer." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}/building/{buildingId}/room/{roomId}", method = RequestMethod.GET)
    @ResponseBody
    public Room getRoomById(
            @ApiParam(value = TERRITORY_ID_PARAM_DESCRIPTION)
            @PathVariable(TERRITORY_ID) String strTerritoryId,
            @ApiParam(value = BUILDING_ID_PARAM_DESCRIPTION)
            @PathVariable(BUILDING_ID) String strBuildingId,
            @ApiParam(value = ROOM_ID_PARAM_DESCRIPTION)
            @PathVariable(ROOM_ID) String strRoomId
    ) throws ThingsboardException {
        checkRelations(List.of(
                assetId(strTerritoryId),
                assetId(strBuildingId),
                assetId(strRoomId)
        ));
        return getById(Room.class, strRoomId);
    }

    @ApiOperation(value = "Delete Room",
            notes = "Deletes the Room and all the relations (from and to the asset). Referencing non-existing asset Id will cause an error." + TENANT_ADMIN_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/territory/{territoryId}/building/{buildingId}/room/{roomId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteRoom(
            @ApiParam(value = TERRITORY_ID_PARAM_DESCRIPTION)
            @PathVariable(TERRITORY_ID) String strTerritoryId,
            @ApiParam(value = BUILDING_ID_PARAM_DESCRIPTION)
            @PathVariable(BUILDING_ID) String strBuildingId,
            @ApiParam(value = ROOM_ID_PARAM_DESCRIPTION)
            @PathVariable(ROOM_ID) String strRoomId
    ) throws ThingsboardException {
        checkRelations(List.of(
                assetId(strTerritoryId),
                assetId(strBuildingId),
                assetId(strRoomId)
        ));

        deleteGeneric(strRoomId);
    }

    @ApiOperation(value = "Find related buildings",
            notes = "Returns all assets that are related to the specific entity. " +
                    "See 'Model' tab of the Parameters for more info.", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}/building/{buildingId}/rooms", method = RequestMethod.GET)
    @ResponseBody
    public PageData<Room> findRoomsByQuery(
            @ApiParam(value = TERRITORY_ID_PARAM_DESCRIPTION)
            @PathVariable(TERRITORY_ID) String strTerritoryId,
            @ApiParam(value = BUILDING_ID_PARAM_DESCRIPTION)
            @PathVariable(BUILDING_ID) String strBuildingId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @ApiParam(value = ASSET_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ASSET_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder
            ) throws ThingsboardException {

        checkRelations(List.of(
                assetId(strTerritoryId),
                assetId(strBuildingId)
        ));

        return getPageData(
                strBuildingId, Room.class, TopologyLevel.ROOM,
                pageSize, page, textSearch, sortProperty, sortOrder);
    }

    @ApiOperation(value = "Delete Device",
            notes = "Deletes the Device and all the relations (from and to the asset). Referencing non-existing asset Id will cause an error." + TENANT_ADMIN_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/territory/{territoryId}/building/{buildingId}/room/{roomId}/device/{deviceId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteRoom(
            @ApiParam(value = TERRITORY_ID_PARAM_DESCRIPTION)
            @PathVariable(TERRITORY_ID) String strTerritoryId,
            @ApiParam(value = BUILDING_ID_PARAM_DESCRIPTION)
            @PathVariable(BUILDING_ID) String strBuildingId,
            @ApiParam(value = ROOM_ID_PARAM_DESCRIPTION)
            @PathVariable(ROOM_ID) String strRoomId,
            @ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION)
            @PathVariable(DEVICE_ID) String strDeviceId
    ) throws ThingsboardException {
        checkRelations(List.of(
                assetId(strTerritoryId),
                assetId(strBuildingId),
                assetId(strRoomId),
                deviceId(strDeviceId)));

        deleteGeneric(strDeviceId);
    }

    @ApiOperation(value = "Create Or Update Device",
            notes = "Creates or Updates the Asset. When creating device, platform generates Asset Id as " + UUID_WIKI_LINK +
                    "The newly created Asset id will be present in the response. " +
                    "Specify existing Asset id to update the device. " +
                    "Referencing non-existing Asset Id will cause 'Not Found' error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}/building/{buildingId}/room/{roomId}/device", method = RequestMethod.POST)
    @ResponseBody
    public TopologyDevice saveDevice(
            @PathVariable(TERRITORY_ID) String strTerritoryId,
            @PathVariable(BUILDING_ID) String strBuildingId,
            @PathVariable(ROOM_ID) String strRoomId,
            @ApiParam(value = "A JSON value representing the device.") @RequestBody TopologyDevice topologyDevice) throws ThingsboardException {
        checkRelations(List.of(
                assetId(strTerritoryId),
                assetId(strBuildingId),
                assetId(strRoomId)
        ));
        Device savedDevice = deviceController.saveDevice(converter.from(topologyDevice), null);
        TopologyDevice savedTopologyDevice = converter.from(savedDevice);

        EntityRelation entityRelation = new EntityRelation();
        entityRelation.setFrom(AssetId.fromString(strRoomId));
        entityRelation.setType(RELATION_TYPE_CONTAINS);
        entityRelation.setTo(EntityIdFactory.getByTypeAndUuid(EntityType.DEVICE, savedTopologyDevice.getId()));

        relationController.saveRelation(entityRelation);

        return savedTopologyDevice;
    }

    @ApiOperation(value = "Get Device",
            notes = "Fetch the Device object based on the provided Asset Id. " +
                    "If the user has the authority of 'Tenant Administrator', the server checks that the asset is owned by the same tenant. " +
                    "If the user has the authority of 'Customer User', the server checks that the asset is assigned to the same customer." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/territory/{territoryId}/building/{buildingId}/room/{roomId}/device/{deviceId}", method = RequestMethod.GET)
    @ResponseBody
    public TopologyDevice getDeviceById(@ApiParam(value = ASSET_ID_PARAM_DESCRIPTION)
                                @PathVariable(TERRITORY_ID) String strTerritoryId,
                                @PathVariable(BUILDING_ID) String strBuildingId,
                                @PathVariable(ROOM_ID) String strRoomId,
                                @PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkRelations(List.of(
                assetId(strTerritoryId),
                assetId(strBuildingId),
                assetId(strRoomId),
                assetId(strDeviceId)
        ));
        return getById(TopologyDevice.class, strDeviceId);
    }

    @ApiOperation(value = "Find related device assignments",
            notes = "Returns all assets that are related to the specific entity. ", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/territory/{territoryId}/building/{buildingId}/room/{roomId}/devices", method = RequestMethod.GET)
    @ResponseBody
    public PageData<TopologyDevice> findDevicesByQuery(
            @PathVariable(TERRITORY_ID) String strTerritoryId,
            @PathVariable(BUILDING_ID) String strBuildingId,
            @PathVariable(ROOM_ID) String strRoomId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @ApiParam(value = ASSET_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ASSET_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        checkRelations(List.of(
                assetId(strTerritoryId),
                assetId(strBuildingId),
                assetId(strRoomId)
        ));

        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = getCurrentUser().getCustomerId();

            var query = NarrowDeviceSearchQuery.builder()
                    .parent(AssetId.fromString(strRoomId))
                    .pageLink(createPageLink(pageSize, page, textSearch, sortProperty, sortOrder))
                    .build();
            var devices = deviceService.findDevicesByTenantIdAndQuery(tenantId, customerId, query);
            return converter.toPage(devices);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @SneakyThrows
    protected <T extends BaseWrapper> T getById(Class<T> targetClass, String id) throws ThingsboardException {
        Asset asset = assetController.getAssetById(id);

        return converter.assign(targetClass.getConstructor().newInstance(), asset);
    }

    protected <T extends BaseWrapper> T save(T posted, String parentId) throws ThingsboardException {
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

    void deleteGeneric(String strId) throws ThingsboardException {
        checkAssetId(AssetId.fromString(strId), Operation.DELETE);
        assetController.deleteAsset(strId);
    }

    private void checkRelations(List<EntityId> levels) throws ThingsboardException {
        Iterator<EntityId> iterator = levels.iterator();
        EntityId fromId = iterator.next();
        while (iterator.hasNext()) {
            EntityId toId = iterator.next();
            try {
                checkEntityId(fromId, Operation.READ);
                checkEntityId(toId, Operation.READ);
                RelationTypeGroup typeGroup = RelationTypeGroup.COMMON;
                boolean exists = relationService.checkRelation(getTenantId(), fromId, toId, RELATION_TYPE_CONTAINS, typeGroup).get();
                if (!exists) {
                    throw new ThingsboardException(
                            format("There is no relation between %s and %s", fromId, toId),
                            ThingsboardErrorCode.ITEM_NOT_FOUND);
                }
                fromId = toId;
            } catch (ExecutionException|InterruptedException e) {
                throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.GENERAL);
            }
        }
    }

    private EntityId assetId(String id) {
        return AssetId.fromString(id);
    }

    private EntityId deviceId(String id) {
        return DeviceId.fromString(id);
    }

    private <T extends BaseWrapper> PageData<T> getPageData(String parentId, Class<T> targetClass, TopologyLevel type, int pageSize, int page, String textSearch, String sortProperty, String sortOrder) throws ThingsboardException {
        var tenantId = getCurrentUser().getTenantId();
        checkTenantId(tenantId, Operation.READ);

        var customerId = getCurrentUser().getCustomerId();
        var parent = nonNull(parentId) ? AssetId.fromString(parentId) : null;

        var query = NarrowAssetSearchQuery.builder()
                .type(type.getKey())
                .parent(parent)
                .pageLink(createPageLink(pageSize, page, textSearch, sortProperty, sortOrder))
                .build();

        checkTenantId(tenantId, Operation.READ);

        PageData<Asset> assetPage = assetService.findAssetsByChildrenQuery(tenantId, customerId, query);
        return converter.toPage(targetClass, assetPage);
    }

}
