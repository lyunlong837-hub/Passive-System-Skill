package com.cceve.passivesystemskill.runtime;

import com.cceve.passivesystemskill.capability.PlayerVariables;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
import com.cceve.passivesystemskill.skilltree.SkillTreeLibrary;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.UUID;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BeastTamingExpHandler {

    private static final int BASE_REQ = 5;
    private static final HashSet<UUID> tamedEntities = new HashSet<>();

    /** 玩家首次驯服生物时触发 */
    @SubscribeEvent
    public static void onAnimalTame(AnimalTameEvent e) {
        if (!(e.getTamer() instanceof ServerPlayer sp))
            return;

        LivingEntity entity = e.getAnimal();
        if (entity == null)
            return;

        var node = SkillTreeLibrary.node(PlayerVariables.SkillId.XunShou);
        if (node == null) {
            return;
        }
        UUID id = entity.getUUID();
        if (tamedEntities.contains(id))
            return; // 防止重复加经验
        tamedEntities.add(id);

        // 经验值 = 生物最大生命值
        double baseMaxHealth = entity.getAttribute(Attributes.MAX_HEALTH) != null
                ? entity.getAttribute(Attributes.MAX_HEALTH).getValue()
                : 10.0;
        int exp = (int) Math.ceil(baseMaxHealth);

        // 玩家获得经验
        gainSkillExp(sp, PlayerVariables.SkillId.XunShou, exp);

        // 应用生命值加成

        // 延迟 5 tick 再加血量
        sp.server.execute(() -> {
            sp.server.execute(() -> {
                if (entity.isAlive()) {
                    applyHealthBonus(sp, entity);
                }
            });
        });
    }

    /** 玩家喂养自己的宠物时刷新血量上限 */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific e) {
        if (!(e.getTarget() instanceof TamableAnimal pet))
            return;
        if (!(e.getEntity() instanceof ServerPlayer sp))
            return;

        // 必须是自己的宠物
        if (!pet.isOwnedBy(sp))
            return;

        ItemStack held = e.getItemStack();
        if (held.isEmpty())
            return;

        applyHealthBonus(sp, pet);

        e.setCancellationResult(InteractionResult.PASS);
    }

    /** 应用生命加成：每级 +2 HP */
    private static void applyHealthBonus(ServerPlayer sp, LivingEntity entity) {
        PlayerVariables vars = PlayerVariablesProvider.get(sp);
        if (vars == null)
            return;

        PlayerVariables.SkillRecord rec = vars.skillMap.get(PlayerVariables.SkillId.XunShou);
        if (rec != null) {
            int extraHealth = rec.level * 2;

            AttributeInstance healthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
            if (healthAttr != null) {
                // ⚡ 从 Attribute 的默认值 (Attribute#getDefaultValue) 或 entity.getType() 定义的基础值取
                double baseHealth = healthAttr.getAttribute().getDefaultValue();

                double newMax = baseHealth + extraHealth;
                healthAttr.setBaseValue(newMax);

                // 确保当前血量不会高于上限
                if (entity.getHealth() > newMax) {
                    entity.setHealth((float) newMax);
                }
            }
        }
    }

    /** 技能经验逻辑 */
    private static void gainSkillExp(ServerPlayer sp, PlayerVariables.SkillId id, int amount) {
        PlayerVariables vars = PlayerVariablesProvider.get(sp);
        if (vars == null)
            return;

        PlayerVariables.SkillRecord rec = vars.skillMap.get(id);
        if (rec == null) {
            var def = SkillTreeLibrary.node(id);
            if (def == null)
                return;
            rec = new PlayerVariables.SkillRecord(0, 0, BASE_REQ, def.maxLevel);
            vars.skillMap.put(id, rec);
        }
        if (rec.expNext <= 0)
            rec.expNext = BASE_REQ;

        int formula = ExpFormula.calcRounded(sp);
        rec.expNow += Math.max(1, amount * formula);

        while (rec.expNow >= rec.expNext && rec.level < SkillTreeLibrary.node(id).maxLevel) {
            rec.expNow -= rec.expNext;
            rec.level++;
            rec.expNext = nextFrom(rec.expNext);
        }
    }

    private static int nextFrom(int prevReq) {
        double prev = Math.max(1.0, prevReq);
        return (int) Math.ceil(prev * 1.1 + 10);
    }
}
