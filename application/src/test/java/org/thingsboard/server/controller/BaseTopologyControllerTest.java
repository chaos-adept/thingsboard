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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.topology.dto.AssetWrapper;
import org.thingsboard.server.common.data.topology.dto.Building;
import org.thingsboard.server.common.data.topology.dto.Room;
import org.thingsboard.server.common.data.topology.dto.Segments;
import org.thingsboard.server.common.data.topology.dto.Territory;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

public abstract class BaseTopologyControllerTest extends AbstractControllerTest {

    private Tenant savedTenant;
    private User tenantAdmin;

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
        Asset asset = new Asset();
        asset.setName("My territory");
        String expectedType = "Territory";
        Territory territory = Territory.from(asset);
        Territory savedTerritory = doPost("/api/topology/territory", territory, Territory.class);

        verifyCreatedAsset(savedTerritory, Segments.TERRITORY.getKey());

        savedTerritory.getAsset().setName("My new asset");
        //todo add test to handle unexpected type
        doPost("/api/topology/territory", Territory.from(savedTerritory.getAsset()), Territory.class);

        Territory foundAsset = doGet("/api/topology/" + savedTerritory.getId(), Territory.class);
        Assert.assertEquals(foundAsset.getAsset().getName(), savedTerritory.getAsset().getName());
    }

    @Test
    public void testSaveBuilding() throws Exception {
        String territoryId = createTerritory().getId();

        Building savedBuilding = createBuilding(territoryId);

        verifyCreatedAsset(savedBuilding, Segments.BUILDING.getKey());

        savedBuilding.getAsset().setName("My new asset");
        //todo add test to handle unexpected type
        doPost("/api/topology/territory/{territoryId}/building", Territory.from(savedBuilding.getAsset()), Territory.class, territoryId);

        Building foundBuilding = doGet("/api/topology/territory/{territoryId}/building/{id}", Building.class,
                territoryId, savedBuilding.getId());
        Assert.assertEquals(foundBuilding.getAsset().getName(), savedBuilding.getAsset().getName());
    }

    @Test
    public void testSaveRoom() throws Exception {
        String territoryId = createTerritory().getId();

        String buildingId = createBuilding(territoryId).getId();

        Asset asset = new Asset();
        asset.setName("My asset");
        Room savedRoom = doPost("/api/topology/territory/{territoryId}/building/{buildingId}/room",
                new Room(asset), Room.class, territoryId, buildingId);

        verifyCreatedAsset(savedRoom, Segments.ROOM.getKey());

        savedRoom.getAsset().setName("My new asset");
        //todo add test to handle unexpected type
        doPost("/api/topology/territory/{territoryId}/building/{buildingId}/room",
                new Room(savedRoom.getAsset()), Room.class, territoryId, buildingId);

        Room foundBuilding = doGet("/api/topology/territory/{territoryId}/building/{buildingId}/room/{id}", Room.class,
                territoryId, buildingId, savedRoom.getId());
        Assert.assertEquals(foundBuilding.getAsset().getName(), savedRoom.getAsset().getName());
    }

    @Test
    public void testGetBuildings() throws Exception {
        String territoryId = createTerritory().getId();

        Building savedBuilding = createBuilding(territoryId);
        Asset savedBuildingAsset = savedBuilding.getAsset();

        var tr = new TypeReference<List<Building>>() {};
        List<Building> loadedAssets = doGetTyped("/api/topology/territory/{territoryId}/buildings",
                tr, territoryId);

        Assert.assertEquals(loadedAssets.size(), 1);
        Assert.assertEquals(loadedAssets.get(0).getAsset().getUuidId(), savedBuildingAsset.getUuidId());
    }

    @Test
    @Ignore
    public void testGetTerritories() {
        Assert.fail("not implemented");
    }


    @Test
    @Ignore
    public void testAddDevicesToRooms() {
        Assert.fail("not implemented");
    }

    private void verifyCreatedAsset(AssetWrapper wrapper, String expectedType) {
        Asset savedAsset = wrapper.getAsset();
        Assert.assertNotNull(savedAsset);
        Assert.assertNotNull(savedAsset.getId());
        Assert.assertTrue(savedAsset.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedAsset.getTenantId());
        Assert.assertNotNull(savedAsset.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedAsset.getCustomerId().getId());
//        Assert.assertEquals(asset.getName(), savedAsset.getName());
//        Assert.assertNull(asset.getType());
        Assert.assertEquals(savedAsset.getType(), expectedType);
    }

    private Building createBuilding(String territoryId) throws Exception {
        Asset asset = new Asset();
        asset.setName("My building");
        Building building = new Building(asset);
        Building savedBuilding = doPost("/api/topology/territory/{territoryId}/building",
                building, Building.class, territoryId);
        return savedBuilding;
    }

    private Territory createTerritory() throws Exception {
        return createTerritory("My Territory");
    }

    private Territory createTerritory(String name) throws Exception {
        Asset territoryAsset = new Asset();
        territoryAsset.setName("My territory");
        Territory territory = new Territory(territoryAsset);
        Territory savedTerritory = doPost("/api/topology/territory", territory, Territory.class);
        return savedTerritory;
    }


}
