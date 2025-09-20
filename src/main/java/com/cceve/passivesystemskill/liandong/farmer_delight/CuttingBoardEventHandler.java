// import net.minecraftforge.eventbus.api.SubscribeEvent;
// import net.minecraftforge.fml.common.Mod;
// import vectorwing.farmersdelight.event.CuttingBoardEvent;

// @Mod.EventBusSubscriber(modid = "passivesystemskill")
// public class CuttingBoardEventHandler {

// @SubscribeEvent
// public static void onCutting(CuttingBoardEvent.Cutting event) {
// // 获取玩家
// var player = event.getPlayer();

// // 获取砧板方块实体
// var blockEntity = event.getBlockEntity();

// // 获取切割的物品
// var tool = event.getTool();
// var input = event.getInputItem();
// var outputs = event.getOutputs();

// // 示例：输出到日志
// System.out.println("[DEBUG] 玩家 " + player.getName().getString()
// + " 用 " + tool.getDisplayName().getString()
// + " 在砧板切割了 " + input.getDisplayName().getString()
// + " -> " + outputs);
// }
// }
