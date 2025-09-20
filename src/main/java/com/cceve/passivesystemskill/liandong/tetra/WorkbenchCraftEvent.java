package com.cceve.passivesystemskill.liandong.tetra;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;
import se.mickelus.tetra.blocks.workbench.WorkbenchTile;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

public class WorkbenchCraftEvent extends Event {
    private final ItemStack targetStack;
    private final ItemStack upgradedStack;
    private final Player player;
    private final Level level;
    private final WorkbenchTile workbenchTile;
    private final ItemStack[] materials;
    private final ItemStack[] materialsAltered;
    private final UpgradeSchematic currentSchematic;
    private final String currentSlot;

    public WorkbenchCraftEvent(ItemStack targetStack, ItemStack upgradedStack,
            Player player, WorkbenchTile workbenchTile,
            ItemStack[] materials, ItemStack[] materialsAltered,
            UpgradeSchematic currentSchematic, String currentSlot) {
        this.targetStack = targetStack;
        this.upgradedStack = upgradedStack;
        this.player = player;
        this.level = player.level();
        this.workbenchTile = workbenchTile;
        this.materials = materials;
        this.materialsAltered = materialsAltered;
        this.currentSchematic = currentSchematic;
        this.currentSlot = currentSlot;
    }

    public ItemStack getTargetStack() {
        return targetStack;
    }

    public ItemStack getUpgradedStack() {
        return upgradedStack;
    }

    public Player getPlayer() {
        return player;
    }

    public Level getLevel() {
        return level;
    }

    public WorkbenchTile getWorkbenchTile() {
        return workbenchTile;
    }

    public ItemStack[] getMaterials() {
        return materials;
    }

    public ItemStack[] getMaterialsAltered() {
        return materialsAltered;
    }

    public UpgradeSchematic getCurrentSchematic() {
        return currentSchematic;
    }

    public String getCurrentSlot() {
        return currentSlot;
    }
}
