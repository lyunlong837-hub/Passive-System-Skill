package com.cceve.passivesystemskill.skilltree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.cceve.passivesystemskill.capability.PlayerVariables.SkillId;
import com.cceve.passivesystemskill.items.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * 技能树“数据层”：节点定义（名称/品质/每级花费/最大等级/图标）与父子关系（支持多父）。
 * 不含坐标；坐标由 AutoLayout 在界面端计算。
 */
public final class SkillTreeLibrary {

        /** 品质决定使用哪张基础框 PNG（着色/叠加由界面端实现） */
        public enum Quality {
                GREEN, BLUE, WHITE, PURPLE, ORANGE, RED
                // 以后可加：PURPLE, ORANGE, ...
        }

        /** 节点定义（与服务端逻辑共享） */
        public static final class NodeDef {
                public final SkillId id;
                public final String name;
                public final int costPerLevel; // 每升 1 级需消耗多少技能点
                public final int maxLevel; // 最大等级（>=1）
                public final Quality quality; // 节点品质（用于选择基础框贴图）
                public final ResourceLocation iconTex; // PNG 图标（可空）
                public final Supplier<? extends ItemLike> iconItem; // 可为 null // 物品图标（可空）
                public final List<Component> description; // 多行描述（可为空）
                // ✅ 新增：动态描述函数
                public final java.util.function.IntFunction<List<Component>> dynamicDesc;

                // 新增：可选角度提示（单位：度）。可为 null。
                public final java.lang.Float angleHintDeg;

                // 兼容：原来 (id,name,cost) 语义 = maxLevel=1, costPerLevel=cost, 默认绿色
                public NodeDef(SkillId id, String name, int cost) {
                        this(id, name, cost, 1, Quality.GREEN, null, null, null);
                }

                // 兼容旧构造：没有动态描述
                public NodeDef(SkillId id, String name, int costPerLevel, int maxLevel, Quality q,
                                ResourceLocation iconTex, Supplier<? extends ItemLike> iconItem,
                                Float angleHintDeg, List<Component> description) {
                        this(id, name, costPerLevel, maxLevel, q, iconTex, iconItem, angleHintDeg, description, null);
                }

                // 便捷：自定义品质
                public NodeDef(SkillId id, String name, int costPerLevel, int maxLevel, Quality q) {
                        this(id, name, costPerLevel, maxLevel, q, null, null, null);
                }

                // 便捷：PNG 图标 + 品质
                public NodeDef(SkillId id, String name, int costPerLevel, int maxLevel, Quality q,
                                ResourceLocation iconTex) {
                        this(id, name, costPerLevel, maxLevel, q, iconTex, null, null);
                }

                // 便捷：物品图标 + 品质
                public NodeDef(SkillId id, String name, int costPerLevel, int maxLevel, Quality q,
                                Supplier<? extends ItemLike> iconItem) {
                        this(id, name, costPerLevel, maxLevel, q, null, iconItem, null);
                }

                // 现有全量构造器(8参)不改签名，内部委托到9参版本，默认空描述
                public NodeDef(SkillId id, String name, int costPerLevel, int maxLevel, Quality q,
                                ResourceLocation iconTex, Supplier<? extends ItemLike> iconItem,
                                Float angleHintDeg) {
                        this(id, name, costPerLevel, maxLevel, q, iconTex, iconItem, angleHintDeg, List.of());
                }

                // 全量构造
                public NodeDef(SkillId id, String name, int costPerLevel, int maxLevel, Quality q,
                                ResourceLocation iconTex, Supplier<? extends ItemLike> iconItem,
                                Float angleHintDeg, List<Component> description,
                                java.util.function.IntFunction<List<Component>> dynamicDesc) {
                        this.id = id;
                        this.name = name;
                        this.costPerLevel = Math.max(0, costPerLevel);
                        this.maxLevel = Math.max(1, maxLevel);
                        this.quality = q == null ? Quality.GREEN : q;
                        this.iconTex = iconTex;
                        this.iconItem = iconItem;
                        this.angleHintDeg = angleHintDeg;
                        this.description = (description == null) ? List.of() : List.copyOf(description);
                        this.dynamicDesc = dynamicDesc;
                }
        }

        /** 边：parent -> child（一个 child 允许出现多个 parent） */
        // SkillTreeLibrary.java 里的 LinkDef
        public static class LinkDef {
                public final SkillId parent;
                public final SkillId child;
                public final int requiredLevel; // 解锁子节点所需的“父节点最低等级”

                public LinkDef(SkillId parent, SkillId child) {
                        this(parent, child, 1); // 旧构造：默认父≥1
                }

                public LinkDef(SkillId parent, SkillId child, int requiredLevel) {
                        this.parent = parent;
                        this.child = child;
                        this.requiredLevel = Math.max(1, requiredLevel);
                }
        }

        public static int requiredParentLevel(SkillId parent, SkillId child) {
                for (LinkDef l : links()) {
                        if (l.parent == parent && l.child == child)
                                return l.requiredLevel;
                }
                return 1;
        }

        private static final Map<SkillId, NodeDef> NODES = new LinkedHashMap<>();
        private static final List<LinkDef> LINKS = new ArrayList<>();
        private static SkillId ROOT = SkillId.mainskill;

        public static Collection<NodeDef> nodes() {
                return Collections.unmodifiableCollection(NODES.values());
        }

        public static List<LinkDef> links() {
                return Collections.unmodifiableList(LINKS);
        }

        public static NodeDef node(SkillId id) {
                return NODES.get(id);
        }

        public static SkillId root() {
                return ROOT;
        }

        public static void setRoot(SkillId id) {
                ROOT = id;
        }

        /** child 的所有父节点（多父支持） */
        public static List<SkillId> parentsOf(SkillId child) {
                List<SkillId> ps = new ArrayList<>();
                for (LinkDef l : LINKS)
                        if (l.child == child)
                                ps.add(l.parent);
                return ps;
        }

        public static void bootstrap() {
                NODES.clear();
                LINKS.clear();
                ROOT = SkillId.mainskill;

                // ==== 节点定义（示例）====
                // 核心
                add(new NodeDef(
                                SkillId.mainskill, "核心", 0, 1, Quality.RED,
                                null, // 第6个参数是纹理，用不到就填 null
                                ModItems.SKILL_CRYSTAL, // ✅ 直接传 RegistryObject 作为 Supplier
                                0f,
                                List.of(
                                                Component.literal("一切的起点").withStyle(net.minecraft.ChatFormatting.GRAY),
                                                Component.literal("无限的可能")
                                                                .withStyle(net.minecraft.ChatFormatting.GRAY))));

                // 力量分支
                add(new NodeDef(
                                SkillId.LiLiang, "力量", 1, 100, Quality.WHITE,
                                null, () -> Items.WOODEN_SWORD, null,
                                List.of(),
                                (level) -> List.of(Component.literal("+" + String.format("%.2f", level * 0.05) + " 攻击力")
                                                .withStyle(ChatFormatting.BLUE))));

                add(new NodeDef(
                                SkillId.WaKuang, "挖矿", 1, 100, Quality.GREEN,
                                null, () -> Items.WOODEN_PICKAXE, null,
                                List.of(),
                                (level) -> List
                                                .of(Component.literal(
                                                                "+" + String.format("%.2f", level * 0.2) + "% 石制品挖掘速度")
                                                                .withStyle(ChatFormatting.BLUE))));

                add(new NodeDef(
                                SkillId.FaMu, "伐木", 1, 100, Quality.GREEN,
                                null, () -> Items.WOODEN_AXE, null,
                                List.of(),
                                (level) -> List
                                                .of(Component.literal(
                                                                "+" + String.format("%.2f", level * 0.2) + "% 木制品挖掘速度")
                                                                .withStyle(ChatFormatting.BLUE))));
                add(new NodeDef(
                                SkillId.QuXie, "驱邪", 1, 100, Quality.GREEN,
                                null, () -> Items.ZOMBIE_HEAD, null,
                                List.of(),
                                (level) -> List.of(Component
                                                .literal("+" + String.format("%.1f", level * 0.1) + "% 对亡灵生物伤害")
                                                .withStyle(ChatFormatting.BLUE))));
                add(new NodeDef(
                                SkillId.DuanZhi, "断肢", 1, 100, Quality.GREEN,
                                null, () -> Items.STRING, null,
                                List.of(),
                                (level) -> List.of(Component
                                                .literal("+" + String.format("%.1f", level * 0.1) + "% 对节肢生物伤害")
                                                .withStyle(ChatFormatting.BLUE))));
                add(new NodeDef(
                                SkillId.Skill_DaLi, "大力", 5, 10, Quality.BLUE,
                                null, () -> Items.IRON_PICKAXE, null,
                                List.of(),
                                (level) -> List
                                                .of(Component.literal("+" + String.format("%d", level * 2) + "% 挖掘速度")
                                                                .withStyle(ChatFormatting.BLUE))));

                // 体质分支
                add(new NodeDef(
                                SkillId.TiZi, "体质", 1, 100, Quality.WHITE,
                                null, () -> Items.APPLE, null,
                                List.of(),
                                (level) -> List.of(Component.literal("+" + String.format("%.1f", level * 0.2) + " 生命值")
                                                .withStyle(ChatFormatting.BLUE))));

                add(new NodeDef(
                                SkillId.ZongJia, "重甲", 1, 100, Quality.GREEN,
                                null, () -> Items.IRON_CHESTPLATE, null,
                                List.of(),
                                (level) -> List.of(Component.literal("+" + String.format("%.2f", level * 0.05) + " 护甲值")
                                                .withStyle(ChatFormatting.BLUE))));

                add(new NodeDef(
                                SkillId.QinJia, "轻甲", 1, 100, Quality.GREEN,
                                null, () -> Items.LEATHER_CHESTPLATE, null,
                                List.of(),
                                (level) -> List.of(
                                                Component.literal("+" + String.format("%.2f", level * 0.05) + " 护甲韧性")
                                                                .withStyle(ChatFormatting.BLUE))));

                // add(new NodeDef(
                // SkillId.GeDang, "格挡", 1, 100, Quality.GREEN,
                // null, () -> Items.SHIELD, null,
                // List.of(),
                // (level) -> List
                // .of(Component.literal("+" + level * 0.05 + " 格挡强度(未完成)")
                // .withStyle(ChatFormatting.BLUE))));

                add(new NodeDef(
                                SkillId.DiaoYu, "钓鱼", 1, 100, Quality.GREEN,
                                null, () -> Items.FISHING_ROD, null,
                                List.of(),
                                (level) -> List.of(Component.literal("钓鱼获得经验").withStyle(ChatFormatting.BLUE))));

                add(new NodeDef(
                                SkillId.Zhongzhi, "种植", 1, 100, Quality.GREEN,
                                null, () -> Items.WOODEN_HOE, null,
                                List.of(),
                                (level) -> List.of(Component.literal("收获获得经验").withStyle(ChatFormatting.BLUE))));

                // 敏捷分支
                add(new NodeDef(
                                SkillId.MinJie, "敏捷", 1, 100, Quality.WHITE,
                                null, () -> Items.RABBIT_FOOT, null,
                                List.of(),
                                (level) -> List.of(
                                                Component.literal("+" + String.format("%.2f", level * 0.02) + " 攻击速度")
                                                                .withStyle(ChatFormatting.BLUE))));

                add(new NodeDef(
                                SkillId.SheJi, "射击", 1, 100, Quality.GREEN,
                                null, () -> Items.BOW, null,
                                List.of(),
                                (level) -> List.of(Component.literal("+" + String.format("%d", level) + " %远程伤害")
                                                .withStyle(ChatFormatting.BLUE))));
                add(new NodeDef(
                                SkillId.XunShou, "驯兽", 1, 100, Quality.GREEN,
                                null, () -> Items.LEAD, null,
                                List.of(),
                                (level) -> List.of(Component.literal("+" + String.format("%d", level * 2) + " 宠物生命值")
                                                .withStyle(ChatFormatting.BLUE))));
                add(new NodeDef(
                                SkillId.QiShu, "骑术", 1, 100, Quality.GREEN,
                                null, () -> Items.LEATHER_HORSE_ARMOR, null,
                                List.of(),
                                (level) -> List.of(Component
                                                .literal("+" + String.format("%.1f", level * 0.1) + "% 骑乘时伤害加成")
                                                .withStyle(ChatFormatting.BLUE))));

                // 智力分支
                add(new NodeDef(
                                SkillId.ZhiLi, "智力", 1, 100, Quality.WHITE,
                                null, () -> Items.BOOK, null,
                                List.of(),
                                (level) -> List.of(Component.literal("+" + String.format("%d", level) + "% 熟练度加成")
                                                .withStyle(ChatFormatting.BLUE))));

                add(new NodeDef(
                                SkillId.XueXi, "附魔", 1, 100, Quality.GREEN,
                                null, () -> Items.ENCHANTING_TABLE, null,
                                List.of(),
                                (level) -> List.of(
                                                Component.literal("-" + String.format("%.1f", level * 0.1) + " 附魔等级需求")
                                                                .withStyle(ChatFormatting.BLUE))));

                // add(new NodeDef(
                // SkillId.LianYao, "炼药", 10000, 1, Quality.GREEN,
                // null, () -> Items.BREWING_STAND, null,
                // List.of(),
                // (level) -> List.of(Component.literal("+" + level + " 炼药效率(未完成)")
                // .withStyle(ChatFormatting.BLUE))));

                add(new NodeDef(
                                SkillId.DuanZao, "锻造", 1, 100, Quality.GREEN,
                                null, () -> Items.ANVIL, null,
                                List.of(),
                                (level) -> List.of(
                                                Component.literal("-" + String.format("%.1f", level * 0.1) + " 铁砧等级消耗")
                                                                .withStyle(ChatFormatting.BLUE))));

                // add(new NodeDef(
                // SkillId.ChuYi, "厨艺", 10000, 1, Quality.GREEN,
                // null, () -> Items.SMOKER, null,
                // List.of(),
                // (level) -> List.of(Component.literal("+" + level + " 厨艺效率(未完成)")
                // .withStyle(ChatFormatting.BLUE))));

                add(new NodeDef(
                                SkillId.Skill_dashi, "百般武艺大师", 10, 5, Quality.PURPLE,
                                null, () -> Items.BRUSH, null,
                                List.of(),
                                (level) -> List.of(Component.literal("+" + String.format("%d", level * 2) + " 全基础熟练度")
                                                .withStyle(ChatFormatting.BLUE))));

                // 精神分支
                add(new NodeDef(
                                SkillId.JinShen, "精神", 1, 100, Quality.WHITE,
                                null, () -> Items.BEACON, null,
                                List.of(),
                                (level) -> List.of(Component.literal("+" + String.format("%d", level * 2) + "% 系统经验加成")
                                                .withStyle(ChatFormatting.BLUE))));
                add(new NodeDef(
                                SkillId.RongYu, "荣誉", 1, 100, Quality.GREEN,
                                null, ModItems.RY_ZWF, null,
                                List.of(),
                                (level) -> List.of(Component
                                                .literal("+" + String.format("%.1f", level * 0.1) + "% 对强敌伤害加成")
                                                .withStyle(ChatFormatting.BLUE))));
                // add(new NodeDef(
                // SkillId.HuaLao, "情商", 1, 100, Quality.GREEN,
                // null, () -> Items.EMERALD, null,
                // List.of(),
                // (level) -> List.of(Component.literal("与村民交易获得经验(未完成)")
                // .withStyle(ChatFormatting.BLUE))));
                add(new NodeDef(
                                SkillId.ShiShui, "嗜睡", 1, 100, Quality.GREEN,
                                null, () -> Items.RED_BED, null,
                                List.of(),
                                (level) -> List.of(Component.literal("睡觉时获得经验")
                                                .withStyle(ChatFormatting.BLUE))));
                add(new NodeDef(
                                SkillId.DaoMei, "倒霉", 1, 100, Quality.GREEN,
                                null, () -> Items.LIGHTNING_ROD, null,
                                List.of(),
                                (level) -> List.of(Component.literal("坏事发生获得经验")
                                                .withStyle(ChatFormatting.BLUE))));

                // 联动分支
                add(new NodeDef(
                                SkillId.mainliandong, "异世界行者", 0, 1, Quality.RED,
                                null, () -> Items.ENDER_EYE, null,
                                List.of(),
                                (level) -> List.of(Component.literal("你与其他世界的链接更紧密了")
                                                .withStyle(ChatFormatting.GRAY))));

                // 铁魔法
                if (net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks")) {
                        add(new NodeDef(
                                        SkillId.IronSpell, "铁魔法研习", 1, 100, Quality.PURPLE,
                                        null, ModItems.IRONSPELL_ZWF, null,
                                        List.of(),
                                        (level) -> List.of(
                                                        Component.literal(
                                                                        "+" + String.format("%d", level * 3) + " 最大法力值")
                                                                        .withStyle(ChatFormatting.BLUE),
                                                        Component.literal("+" + String.format("%.1f", level * 0.1)
                                                                        + "% 法力值回复")
                                                                        .withStyle(ChatFormatting.BLUE),
                                                        Component.literal("+" + String.format("%.2f", level * 0.05)
                                                                        + "% 法术强度")
                                                                        .withStyle(ChatFormatting.BLUE))));
                        link(SkillId.mainliandong, SkillId.IronSpell);
                        add(new NodeDef(
                                        SkillId.IronSpellFaQiang, "铁魔法造诣", 10, 10, Quality.PURPLE,
                                        null, ModItems.IRONSPELL_ZWFB, null,
                                        List.of(),
                                        (level) -> List.of(
                                                        Component.literal(String.format("%d", level * 1) + "% 几率施法时:")
                                                                        .withStyle(ChatFormatting.BLUE),
                                                        Component
                                                                        .literal("+" + String.format("%.1f",
                                                                                        level * 0.5) + "% 法术强度(00:"
                                                                                        + String.format("%d", level)
                                                                                        + ")")
                                                                        .withStyle(ChatFormatting.BLUE))));
                        link(SkillId.IronSpell, SkillId.IronSpellFaQiang, 20);
                }

                // TETRA
                if (net.minecraftforge.fml.ModList.get().isLoaded("tetra")) {
                        add(new NodeDef(
                                        SkillId.tetra, "远古锻造研习", 1, 100, Quality.PURPLE,
                                        null, ModItems.TETRA_ZWFB, null,
                                        List.of(),
                                        (level) -> List.of(
                                                        Component.literal(
                                                                        "使用加工台获得经验")
                                                                        .withStyle(ChatFormatting.BLUE))));
                        link(SkillId.mainliandong, SkillId.tetra);
                        if (net.minecraftforge.fml.ModList.get().isLoaded("tetra")) {
                                add(new NodeDef(
                                                SkillId.tetra_quality, "稳定器研究", 25, 5, Quality.ORANGE,
                                                null, ModItems.TETRA_ZWF, null,
                                                List.of(),
                                                (level) -> List.of(
                                                                Component.literal("获得熟练锻造 " + toRoman(level))
                                                                                .withStyle(ChatFormatting.BLUE))));
                                link(SkillId.tetra, SkillId.tetra_quality, 50);
                        }
                }

                // ==== 父子关系（支持多父）====
                // link(A, B)：从 A 到 B 加一条边，表示 B 的父节点有 A

                link(SkillId.mainskill, SkillId.LiLiang);
                link(SkillId.mainskill, SkillId.TiZi);
                link(SkillId.mainskill, SkillId.MinJie);
                link(SkillId.mainskill, SkillId.ZhiLi);
                link(SkillId.mainskill, SkillId.JinShen);
                link(SkillId.mainskill, SkillId.mainliandong);

                // 力量
                link(SkillId.LiLiang, SkillId.WaKuang, 10);
                link(SkillId.LiLiang, SkillId.FaMu, 10);
                link(SkillId.LiLiang, SkillId.QuXie, 10);
                link(SkillId.LiLiang, SkillId.DuanZhi, 10);
                // 小技能
                link(SkillId.FaMu, SkillId.Skill_DaLi, 10);
                link(SkillId.WaKuang, SkillId.Skill_DaLi, 10);
                // 体质
                link(SkillId.TiZi, SkillId.ZongJia, 10);
                link(SkillId.TiZi, SkillId.QinJia, 10);
                link(SkillId.TiZi, SkillId.GeDang, 10);
                link(SkillId.TiZi, SkillId.Zhongzhi, 10);
                link(SkillId.TiZi, SkillId.DiaoYu, 10);

                // 敏捷
                link(SkillId.MinJie, SkillId.SheJi, 10);
                link(SkillId.MinJie, SkillId.XunShou, 10);
                link(SkillId.MinJie, SkillId.QiShu, 10);
                // 智力
                link(SkillId.ZhiLi, SkillId.XueXi, 10);
                link(SkillId.ZhiLi, SkillId.DuanZao, 10);
                link(SkillId.ZhiLi, SkillId.ChuYi, 10);
                link(SkillId.ZhiLi, SkillId.LianYao, 10);
                // 小技能
                link(SkillId.XueXi, SkillId.Skill_dashi, 20);
                link(SkillId.DuanZao, SkillId.Skill_dashi, 20);

                // 精神
                link(SkillId.JinShen, SkillId.RongYu, 10);
                link(SkillId.JinShen, SkillId.HuaLao, 10);
                link(SkillId.JinShen, SkillId.ShiShui, 10);
                link(SkillId.JinShen, SkillId.DaoMei, 10);

        }

        private static void add(NodeDef def) {
                if (NODES.containsKey(def.id))
                        throw new IllegalStateException("重复 SkillId: " + def.id);
                NODES.put(def.id, def);
        }

        private static void link(SkillId parent, SkillId child) {
                LINKS.add(new LinkDef(parent, child)); // 默认父 ≥ 1
        }

        // ✅ 新增：需要父达到指定等级
        private static void link(SkillId parent, SkillId child, int requiredLevel) {
                LINKS.add(new LinkDef(parent, child, requiredLevel));
        }

        // 整点帅的罗马字符
        private static String toRoman(int number) {
                return switch (number) {
                        case 0 -> "";
                        case 1 -> "Ⅰ";
                        case 2 -> "Ⅱ";
                        case 3 -> "Ⅲ";
                        case 4 -> "Ⅳ";
                        case 5 -> "Ⅴ";
                        default -> String.valueOf(number);
                };
        }

}
