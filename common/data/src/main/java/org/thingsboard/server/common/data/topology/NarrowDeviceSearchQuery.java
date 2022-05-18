package org.thingsboard.server.common.data.topology;

import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageLink;

import javax.validation.constraints.NotNull;

@Data
@SuperBuilder
public class NarrowDeviceSearchQuery {
    @NotNull
    private final EntityId parent;

    @NotNull
    private final PageLink pageLink;
}
