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

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.topology.dto.*;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

public abstract class BaseTopologyControllerTest extends AbstractControllerTest {

    private Tenant savedTenant;
    private User tenantAdmin;

    @Autowired
    protected RelationService relationService;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveTerritory() throws Exception {
        Territory territory = Territory.builder().name("My territory").build();
        Territory savedTerritory = doPost("/api/topology/territory", territory, Territory.class);

        verifyCreatedAsset(savedTerritory, TopologyLevel.TERRITORY.getKey());

        savedTerritory.setName("My new asset");
        doPost("/api/topology/territory", savedTerritory, Territory.class);

        Territory found = doGet("/api/topology/territory/{id}", Territory.class, savedTerritory.getId());
        Assert.assertEquals(found.getName(), savedTerritory.getName());
    }

    @Test
    public void testSaveBuilding() throws Exception {
        String territoryId = createTerritory().getId();

        Building savedBuilding = createBuilding(territoryId);

        Assert.assertTrue(relationService.checkRelation(
                tenantId,
                AssetId.fromString(territoryId),
                AssetId.fromString(savedBuilding.getId()),
                EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get());

        savedBuilding.setName("My new asset");
        doPost("/api/topology/territory/{territoryId}/building", savedBuilding, Building.class, territoryId);

        Building foundBuilding = doGet("/api/topology/territory/{territoryId}/building/{id}", Building.class,
                territoryId, savedBuilding.getId());
        Assert.assertEquals(foundBuilding.getName(), savedBuilding.getName());
    }

    @Test
    public void testSaveRoom() throws Exception {
        String territoryId = createTerritory().getId();
        String buildingId = createBuilding(territoryId).getId();

        Room savedRoom = createRoom(territoryId, buildingId);

        Assert.assertTrue(relationService.checkRelation(
                tenantId,
                AssetId.fromString(territoryId),
                AssetId.fromString(buildingId),
                EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get());
        Assert.assertTrue(relationService.checkRelation(
                tenantId,
                AssetId.fromString(buildingId),
                AssetId.fromString(savedRoom.getId()),
                EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get());

        savedRoom.setName("My new asset");
        doPost("/api/topology/territory/{territoryId}/building/{buildingId}/room",
                savedRoom, Room.class, territoryId, buildingId);

        Room foundRoom = doGet("/api/topology/territory/{territoryId}/building/{buildingId}/room/{id}", Room.class,
                territoryId, buildingId, savedRoom.getId());
        Assert.assertEquals(foundRoom.getName(), savedRoom.getName());
    }

    private Room createRoom(String territoryId, String buildingId) throws Exception {
        Room newRoom = Room.builder().name("New Room").build();
        Room savedRoom = doPost("/api/topology/territory/{territoryId}/building/{buildingId}/room",
                newRoom, Room.class, territoryId, buildingId);
        return savedRoom;
    }

    @Test
    public void testGetBuildings() throws Exception {
        String territoryId = createTerritory().getId();

        Building savedBuilding = createBuilding(territoryId);

        var tr = new TypeReference<List<Building>>() {};
        List<Building> loadedAssets = doGetTyped("/api/topology/territory/{territoryId}/buildings",
                tr, territoryId);

        Assert.assertEquals(loadedAssets.size(), 1);
        Assert.assertEquals(loadedAssets.get(0).getId(), savedBuilding.getId());
    }

    @Test
    @Ignore
    public void testGetTerritories() {
        Assert.fail("not implemented");
    }


    @Test
    public void testAddDevicesToRooms() throws Exception {
        Territory territory = createTerritory();
        Building building = createBuilding(territory.getId());
        Room room = createRoom(territory.getId(), building.getId());

        Assert.assertTrue(relationService.checkRelation(
                tenantId,
                AssetId.fromString(territory.getId()),
                AssetId.fromString(building.getId()),
                EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get());
        Assert.assertTrue(relationService.checkRelation(
                tenantId,
                AssetId.fromString(building.getId()),
                AssetId.fromString(room.getId()),
                EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get());

        TopologyDevice device = new TopologyDevice();
        device.setName("My device");
        device.setType("default");

        TopologyDevice savedDevice = doPost(
                "/api/topology/territory/{territoryId}/building/{buildingId}/room/{roomId}/device",
                device, TopologyDevice.class,
                territory.getId(), building.getId(), room.getId());

        Assert.assertTrue(relationService.checkRelation(
                tenantId,
                AssetId.fromString(room.getId()),
                DeviceId.fromString(savedDevice.getId()),
                EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get());

        var tr = new TypeReference<List<TopologyDevice>>() {};
        List<TopologyDevice> deviceList = doGetTyped(
                "/api/topology/territory/{territoryId}/building/{buildingId}/room/{roomId}/devices",
                tr,
                territory.getId(), building.getId(), room.getId());
        Assert.assertEquals(deviceList.get(0).getId(), savedDevice.getId());
    }

    @SneakyThrows
    private void verifyCreatedAsset(BaseWrapper wrapper, String expectedType) {
        Asset savedAsset = doGet("/api/asset/" + wrapper.getId(), Asset.class);
        Assert.assertNotNull(savedAsset);
        Assert.assertNotNull(savedAsset.getId());
        Assert.assertTrue(savedAsset.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedAsset.getTenantId());
        Assert.assertNotNull(savedAsset.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedAsset.getCustomerId().getId());
        Assert.assertEquals(wrapper.getName(), savedAsset.getName());
        Assert.assertEquals(savedAsset.getType(), wrapper.getType());
        Assert.assertEquals(savedAsset.getType(), expectedType);
    }

    private Building createBuilding(String territoryId) throws Exception {
        Building building = Building.builder().name("My building").build();
        Building savedBuilding = doPost("/api/topology/territory/{territoryId}/building",
                building, Building.class, territoryId);
        return savedBuilding;
    }

    private Territory createTerritory() throws Exception {
        return createTerritory("My Territory");
    }

    private Territory createTerritory(String name) throws Exception {
        Territory territory = Territory.builder().name("My territory").build();
        return doPost("/api/topology/territory", territory, Territory.class);
    }


}
