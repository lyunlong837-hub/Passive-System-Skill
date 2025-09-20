// package com.cceve.passivesystemskill.runtime;

// import com.cceve.passivesystemskill.capability.PlayerVariables;
// import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
// import com.cceve.passivesystemskill.pss_main;
// import com.cceve.passivesystemskill.skilltree.SkillTreeLibrary;
// import net.minecraft.server.level.ServerPlayer;
// import net.minecraft.world.entity.ai.attributes.AttributeModifier;
// import net.minecraft.world.entity.ai.attributes.Attributes;
// import net.minecraftforge.event.TickEvent;
// import net.minecraftforge.eventbus.api.SubscribeEvent;
// import net.minecraftforge.fml.common.Mod;
// import net.minecraft.network.chat.Component;
// import java.util.UUID;

// @Mod.EventBusSubscriber(modid = pss_main.MODID, bus =
// Mod.EventBusSubscriber.Bus.FORGE)
// public class SneakExpHandler {

// private static final int BASE_REQ = 5;
// private static final UUID SNEAK_SPEED_MODIFIER =
// UUID.fromString("94FE7F59-2D7A-7E08-5D7E-1D2432ECDC64");

// @SubscribeEvent
// public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
// if (!(e.player instanceof ServerPlayer sp))
// return;
// if (e.phase != TickEvent.Phase.END)
// return;

// PlayerVariables vars = PlayerVariablesProvider.get(sp);
// if (vars == null)
// return;

// var def = SkillTreeLibrary.node(PlayerVariables.SkillId.QianXing); // 你要自己在
// SkillTreeLibrary 里定义
// if (def == null)
// return;

// // 检测潜行
// if (sp.isShiftKeyDown()) {
// // 判断位置是否有变化
// double dx = sp.getX() - sp.xo;
// double dz = sp.getZ() - sp.zo;
// double distSq = dx * dx + dz * dz;

// if (distSq > 0.0001) { // 有移动
// sp.sendSystemMessage(Component.literal("潜行移动中"));
// if (sp.tickCount % 20 == 0) {
// gainSkillExp(sp, PlayerVariables.SkillId.QianXing, 1);
// }
// }
// }

// // 根据等级增加潜行速度
// var rec = vars.skillMap.get(PlayerVariables.SkillId.QianXing);
// if (rec != null && rec.level > 0) {
// double bonus = 0.02 * rec.level; // 每级 +2% 速度
// var inst = sp.getAttribute(Attributes.MOVEMENT_SPEED);
// if (inst != null) {
// // 先移除旧的 modifier，避免叠加
// inst.removeModifier(SNEAK_SPEED_MODIFIER);
// // 重新加上
// inst.addTransientModifier(new AttributeModifier(
// SNEAK_SPEED_MODIFIER,
// "Sneak Speed Bonus",
// bonus,
// AttributeModifier.Operation.MULTIPLY_TOTAL));
// }
// }
// }

// private static void gainSkillExp(ServerPlayer sp, PlayerVariables.SkillId id,
// int amount) {
// PlayerVariables vars = PlayerVariablesProvider.get(sp);
// if (vars == null)
// return;

// var def = SkillTreeLibrary.node(id);
// if (def == null)
// return;

// PlayerVariables.SkillRecord rec = vars.skillMap.get(id);
// if (rec == null) {
// rec = new PlayerVariables.SkillRecord(0, 0, BASE_REQ, def.maxLevel);
// vars.skillMap.put(id, rec);
// }
// if (rec.expNext <= 0)
// rec.expNext = BASE_REQ;

// int formula = ExpFormula.calcRounded(sp);
// rec.expNow += Math.max(1, amount * formula);
// sp.sendSystemMessage(Component.literal("潜行：经验 " + amount + " 点"));
// while (rec.expNow >= rec.expNext && rec.level < def.maxLevel) {
// rec.expNow -= rec.expNext;
// rec.level++;
// rec.expNext = nextFrom(rec.expNext);
// }
// }

// private static int nextFrom(int prevReq) {
// double prev = Math.max(1.0, prevReq);
// return (int) Math.ceil(prev * 1.1 + 10);
// }
// } 用不了BUG