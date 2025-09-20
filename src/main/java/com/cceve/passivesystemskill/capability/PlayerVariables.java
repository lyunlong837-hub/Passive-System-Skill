package com.cceve.passivesystemskill.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.Map;
import com.cceve.passivesystemskill.network.SyncPlayerVariablesPacket;
import com.cceve.passivesystemskill.network.ModNetwork;

public class PlayerVariables {

    /** 全局经验倍率 从config提取（基准 1.0；药水/效果可改为 1.1~1.5 等） */
    public double exp_global_persent = 1.0;

    /** 你自己的货币/点数容器 */
    public static class Currencies {
        public double skillPoints;

        public Currencies copy() {
            Currencies c = new Currencies();
            c.skillPoints = this.skillPoints;
            return c;
        }

        public CompoundTag saveNBT() {
            CompoundTag t = new CompoundTag();
            t.putDouble("skillPoints", skillPoints);
            return t;
        }

        public void loadNBT(CompoundTag t) {
            this.skillPoints = t.getDouble("skillPoints");
        }
    }

    /** 技能记录（示例：等级/当前经验/下一等级所需经验） */
    public static class SkillRecord {
        public int level;
        public int expNow;
        public int expNext;
        public int maxLevel; // 不再手动赋值，改为从 SkillTreeLibrary 取

        public SkillRecord() {
        }

        public SkillRecord(int level, int expNow, int expNext, int maxLevel) {
            this.level = level;
            this.expNow = expNow;
            this.expNext = expNext;
            this.maxLevel = maxLevel; // 这里还是可以保留，用于读档
        }

        public void syncMaxLevel(PlayerVariables.SkillId id) {
            var def = com.cceve.passivesystemskill.skilltree.SkillTreeLibrary.node(id);
            if (def != null) {
                this.maxLevel = def.maxLevel;
            }
        }

        public SkillRecord copy() {
            return new SkillRecord(this.level, this.expNow, this.expNext, this.maxLevel);
        }

        public CompoundTag saveNBT() {
            CompoundTag t = new CompoundTag();
            t.putInt("level", level);
            t.putInt("expNow", expNow);
            t.putInt("expNext", expNext);
            // maxLevel 不需要保存，重新载入时会从 SkillTreeLibrary 同步
            return t;
        }

        public void loadNBT(CompoundTag t) {
            this.level = t.getInt("level");
            this.expNow = t.contains("expNow") ? t.getInt("expNow") : 0;
            this.expNext = t.contains("expNext") ? t.getInt("expNext") : 5;
            // load 后统一刷新
            // 注意：id 需要外部传进来再调用 syncMaxLevel
        }
    }

    /** 你的 SkillId 枚举 */
    public enum SkillId {
        mainskill,
        // 力量
        LiLiang, WaKuang, FaMu /* 伐木 */, QuXie, DuanZhi, Skill_DaLi,

        // 体质
        TiZi, ZongJia, QinJia, GeDang,
        DiaoYu, Zhongzhi,

        // 敏捷
        MinJie, SheJi, TouZhi, QianXing, HuaXiang,
        XunShou, QiShu,

        // 智力
        ZhiLi, XueXi, DuanZao, ChuYi, LianYao,
        Skill_dashi,

        // 精神
        JinShen, HuaLao, ShiShui,
        DaoMei, RongYu,

        // 联动
        mainliandong,

        // 铁魔法
        IronSpell, IronSpellFaQiang, IronSpellRegen,

        // tetra
        tetra, tetra_quality

        /* …… */
    }

    /** 技能表：使用 EnumMap 存储 */
    public static class PlayerSkills {
        private final Map<SkillId, SkillRecord> map = new EnumMap<>(SkillId.class);

        public SkillRecord get(SkillId id) {
            return map.get(id);
        }

        public void put(SkillId id, SkillRecord rec) {
            map.put(id, rec);
        }

        public boolean contains(SkillId id) {
            return map.containsKey(id);
        }

        public Iterable<Map.Entry<SkillId, SkillRecord>> entries() {
            return map.entrySet();
        }

        public void clear() {
            map.clear();
        }

        /** 深拷贝 */
        public void copyFrom(PlayerSkills other) {
            this.clear();
            for (var e : other.map.entrySet()) {
                this.map.put(e.getKey(), e.getValue() == null ? null : e.getValue().copy());
            }
        }

        public CompoundTag saveNBT() {
            CompoundTag t = new CompoundTag();
            for (var e : map.entrySet()) {
                SkillRecord rec = e.getValue();
                if (rec != null) {
                    t.put(e.getKey().name(), rec.saveNBT());
                }
            }
            return t;
        }

        public void loadNBT(CompoundTag t) {
            this.clear();
            for (SkillId id : SkillId.values()) {
                if (t.contains(id.name())) {
                    SkillRecord r = new SkillRecord();
                    r.loadNBT(t.getCompound(id.name()));
                    r.syncMaxLevel(id); // ← 这里保证maxLevel和SkillTreeLibrary一致
                    this.map.put(id, r);
                }
            }
        }
    }

    // ===== PlayerVariables 实际字段 =====
    public final Currencies currencies = new Currencies();
    public final PlayerSkills skillMap = new PlayerSkills();

    // —— 存档/读档 —— //
    public CompoundTag saveNBT() {
        CompoundTag t = new CompoundTag();
        t.put("currencies", currencies.saveNBT());
        t.put("skillMap", skillMap.saveNBT());
        t.putDouble("exp_global_persent", this.exp_global_persent); // 保存倍率（默认 1.0）

        return t;
    }

    public void loadNBT(CompoundTag t) {
        if (t.contains("currencies"))
            currencies.loadNBT(t.getCompound("currencies"));
        if (t.contains("skillMap"))
            skillMap.loadNBT(t.getCompound("skillMap"));
        this.exp_global_persent = t.contains("exp_global_persent")
                ? t.getDouble("exp_global_persent")
                : 1.0; // 读不到时默认 1.0
    }

    /** Clone 时深拷贝（维度切换/非死亡转移时保持一致；死亡重生是否重置由事件控制） */
    public void copyFrom(PlayerVariables other) {
        this.currencies.skillPoints = other.currencies.skillPoints;
        this.skillMap.copyFrom(other.skillMap);
        this.exp_global_persent = other.exp_global_persent;
    }

    public void sync(ServerPlayer player) {
        // 把当前这个 PlayerVariables 对象发到客户端
        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncPlayerVariablesPacket(this));
    }

}
