package com.cceve.passivesystemskill.runtime;

import com.cceve.passivesystemskill.capability.PlayerVariables;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
import com.cceve.passivesystemskill.pss_main;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.cceve.passivesystemskill.skilltree.SkillTreeLibrary;

/**
 * 方块被破坏时结算：硬度 * ExpFormula.calcRounded(player)
 * 启用条件：
 * - 主手是矿镐 且 挖矿等级>=1 -> 增加挖矿经验 (WaKuang)
 * - 主手是斧头 且 伐木等级>=1 -> 增加伐木经验 (FaMu)
 * 升级规则：
 * - 初始需求 BASE_REQ
 * - 下一等级需求 = ceil( 上一级需求^1.3 + 5 )
 */
@Mod.EventBusSubscriber(modid = pss_main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MiningWoodcuttingExpHandler {

    private MiningWoodcuttingExpHandler() {
    }

    /** 0→1级的基础需求 */
    private static final int BASE_REQ = 5;

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer sp))
            return;
        if (sp.isCreative() || sp.isSpectator())
            return; // 创造/旁观不记经验

        var node = SkillTreeLibrary.node(PlayerVariables.SkillId.WaKuang);
        if (node == null) {
            return;
        }
        ItemStack main = sp.getMainHandItem();
        boolean isPick = main.getItem() instanceof PickaxeItem;
        boolean isAxe = main.getItem() instanceof AxeItem;
        if (!isPick && !isAxe)
            return;

        PlayerVariables vars = PlayerVariablesProvider.get(sp);
        if (vars == null)
            return;

        if (isPick) {
            int lvMine = getLevel(vars, PlayerVariables.SkillId.WaKuang);
            if (lvMine < 1)
                return; // 挖矿未启用
            int gain = computeGain(e, sp);
            if (gain > 0)
                gainSkillExp(sp, PlayerVariables.SkillId.WaKuang, gain);
        } else { // isAxe
            int lvWood = getLevel(vars, PlayerVariables.SkillId.FaMu);
            if (lvWood < 1)
                return; // 伐木未启用
            int gain = computeGain(e, sp);
            if (gain > 0)
                gainSkillExp(sp, PlayerVariables.SkillId.FaMu, gain);
        }
    }

    /** 本次应得经验：方块硬度 * 公式；硬度<=0 时返回0 */
    // 替换 MiningWoodcuttingExpHandler.computeGain(...)
    private static int computeGain(BlockEvent.BreakEvent e, ServerPlayer sp) {
        // getLevel() 是 LevelAccessor，不是 Level
        net.minecraft.world.level.LevelAccessor level = e.getLevel();
        BlockState state = e.getState();

        // BlockState 不会是 null，这里仅作防御
        if (state == null)
            return 0;

        // 1.20.1 的签名：getDestroySpeed(BlockGetter/LevelReader, BlockPos)
        float hardness = state.getDestroySpeed(level, e.getPos());
        if (hardness <= 0f)
            return 0;

        int formula = ExpFormula.calcRounded(sp);
        int gain = (int) Math.round(hardness * formula);
        return Math.max(gain, 1);
    }

    /** 给指定技能加经验；满则升级（递推：next = ceil(prev^1.3 + 5)） */
    private static void gainSkillExp(ServerPlayer sp, PlayerVariables.SkillId id, int amount) {
        PlayerVariables vars = PlayerVariablesProvider.get(sp);
        if (vars == null)
            return;

        PlayerVariables.SkillRecord rec = vars.skillMap.get(id);
        if (rec == null) {
            rec = new PlayerVariables.SkillRecord(0, 0, BASE_REQ, SkillTreeLibrary.node(id).maxLevel);
            vars.skillMap.put(id, rec);
        }
        if (rec.expNext <= 0)
            rec.expNext = BASE_REQ;

        rec.expNow += amount;

        // 可能一次升多级
        while (rec.expNow >= rec.expNext && rec.level < SkillTreeLibrary.node(id).maxLevel) {
            rec.expNow -= rec.expNext;
            rec.level++;
            rec.expNext = nextFrom(rec.expNext);
        }

    }

    /** 上一级需求 -> 下一等级需求：ceil(prev^1.3 + 5) */
    private static int nextFrom(int prevReq) {
        double prev = Math.max(1.0, prevReq);
        double next = (int) Math.ceil(prev * 1.1 + 10);
        // 用天花板取整，保证严格增长
        return (int) Math.ceil(next);
    }

    /** 读取某技能当前等级（无记录返回0） */
    private static int getLevel(PlayerVariables vars, PlayerVariables.SkillId id) {
        PlayerVariables.SkillRecord r = vars.skillMap.get(id);
        return r == null ? 0 : r.level;
    }
}
