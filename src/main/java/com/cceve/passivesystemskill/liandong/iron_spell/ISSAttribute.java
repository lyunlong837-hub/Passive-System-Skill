package com.cceve.passivesystemskill.liandong.iron_spell;

import java.util.UUID;

import com.cceve.passivesystemskill.capability.PlayerVariables;
import com.cceve.passivesystemskill.capability.PlayerVariables.SkillId;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import net.minecraftforge.fml.ModList;

public final class ISSAttribute {

    // ===== 固定 UUID：避免叠加/残留 =====
    private static final UUID UUID_ISSmain_mana = UUID.fromString("5EB63AB5-2786-4999-2A0A-5AF3B8752E71");
    private static final UUID UUID_ISSmain_power = UUID.fromString("A130CE39-98D0-C75A-EA9D-0A22AA286024");
    private static final UUID UUID_ISSmain_manare = UUID.fromString("9E627154-BDE2-80A7-F203-185BF3D641AA");
    private static final UUID UUID_ISSmain_power2 = UUID.fromString("E65F9F87-B256-570E-3F0B-E0118863F616");

    // ===== 数值常量（可按需调整） =====
    private static final double ISSmain_mana_PER_LEVEL = 3; // 每级 +3 法力
    private static final double ISSmain_power_PER_LEVEL = 0.0005;// 每级 +0.0005 法强
    private static final double ISSmain_manare_PER_LEVEL = 0.001; // 每级 +0.001 法力回复

    private ISSAttribute() {
    }

    /** 安全调用，捕获所有异常 */
    public static void applyAllSafe(ServerPlayer sp) {
        try {
            applyAll(sp);
        } catch (Throwable ignored) {
        }
    }

    /** 应用所有属性修饰符 */
    public static void applyAll(ServerPlayer sp) {
        if (!ModList.get().isLoaded("irons_spellbooks"))
            return;

        PlayerVariables vars = PlayerVariablesProvider.get(sp);
        if (vars == null)
            return;

        applyManaBonus(sp, vars);
        applyManaPower(sp, vars);
        applyManaReBonus(sp, vars);
    }

    /** 铁魔法：每级 +3 法力 */
    private static void applyManaBonus(ServerPlayer sp, PlayerVariables vars) {
        var attr = getISSAttribute(sp, "MAX_MANA");
        if (attr == null)
            return;

        int lv = levelOf(vars, SkillId.IronSpell);
        double bonus = lv * ISSmain_mana_PER_LEVEL;
        setAdditive(attr, UUID_ISSmain_mana, "pss_liandong_iss_mana_bonus", bonus);
    }

    /** 铁魔法：每级 +0.001 法力回复 */
    private static void applyManaReBonus(ServerPlayer sp, PlayerVariables vars) {
        var attr = getISSAttribute(sp, "MANA_REGEN");
        if (attr == null)
            return;

        int lv = levelOf(vars, SkillId.IronSpell);
        double bonus = lv * ISSmain_manare_PER_LEVEL;
        setAdditive(attr, UUID_ISSmain_manare, "pss_liandong_iss_manare_bonus", bonus);
    }

    /** 铁魔法：每级 +0.0005 法强 */
    private static void applyManaPower(ServerPlayer sp, PlayerVariables vars) {
        var attr = getISSAttribute(sp, "SPELL_POWER");
        if (attr == null)
            return;

        int lv = levelOf(vars, SkillId.IronSpell);
        double bonus = lv * ISSmain_power_PER_LEVEL;
        setAdditive(attr, UUID_ISSmain_power, "pss_liandong_iss_manapower_bonus", bonus);
    }

    /** 铁魔法2：每级 +额外法强 */
    public static void magicPowerBonus(ServerPlayer sp, PlayerVariables vars, int num) {
        var attr = getISSAttribute(sp, "SPELL_POWER");
        if (attr == null)
            return;

        int lv = levelOf(vars, SkillId.IronSpellFaQiang);
        double bonus = num * 0.005 * lv;
        setAdditive(attr, UUID_ISSmain_power2, "pss_liandong_iss_manapower2_bonus", bonus);
    }

    // ===== 工具函数 =====

    /** 获取 Iron’s Spellbooks 的属性（反射，不会导致崩溃） */
    private static AttributeInstance getISSAttribute(ServerPlayer sp, String fieldName) {
        try {
            // io.redspace.ironsspellbooks.api.registry.AttributeRegistry
            Class<?> cls = Class.forName("io.redspace.ironsspellbooks.api.registry.AttributeRegistry");
            var field = cls.getField(fieldName); // 例如 MAX_MANA
            Object supplier = field.get(null); // Forge RegistryObject<Attribute>
            Object attrObj = supplier.getClass().getMethod("get").invoke(supplier);
            if (attrObj instanceof net.minecraft.world.entity.ai.attributes.Attribute attr) {
                return sp.getAttribute(attr);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** 空安全读等级 */
    private static int levelOf(PlayerVariables vars, SkillId id) {
        if (vars == null || vars.skillMap == null || id == null)
            return 0;
        var rec = vars.skillMap.get(id);
        return rec != null ? rec.level : 0;
    }

    /** 设置“加法”修饰符（先移除旧的再添加） */
    private static void setAdditive(AttributeInstance inst, UUID uuid, String name, double amount) {
        var old = inst.getModifier(uuid);
        if (old != null)
            inst.removeModifier(uuid);
        if (amount != 0.0) {
            inst.addPermanentModifier(new AttributeModifier(uuid, name, amount, AttributeModifier.Operation.ADDITION));
        }
    }
}
