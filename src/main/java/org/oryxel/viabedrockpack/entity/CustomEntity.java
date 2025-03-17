package org.oryxel.viabedrockpack.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.oryxel.viabedrockpack.entity.renderer.model.CustomEntityModel;

public class CustomEntity extends MobEntity {
    public CustomEntityModel model;
    public Identifier texture;

    public CustomEntity(final EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }
}
