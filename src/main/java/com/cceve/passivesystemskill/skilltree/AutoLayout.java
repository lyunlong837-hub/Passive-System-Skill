package com.cceve.passivesystemskill.skilltree;

import com.cceve.passivesystemskill.capability.PlayerVariables.SkillId;

import java.util.*;

/** 简单径向布局：根在(0,0)，子节点均匀分布在父节点外一圈；保留父方向缺口避免重叠。 */
public final class AutoLayout {

    public static final class Point {
        public final int x, y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * @param root         根节点
     * @param links        父子边列表（child 可有多个 parent）
     * @param radiusStep   每一层的半径步长（像素）
     * @param parentGapDeg 与父节点连线方向的避让角（度，非根时可留出缺口）
     */
    public static Map<SkillId, Point> compute(SkillId root,
            List<SkillTreeLibrary.LinkDef> links,
            int radiusStep,
            double parentGapDeg) {
        // 1) 建图：parents / children / all 节点集合
        Map<SkillId, List<SkillId>> parents = new HashMap<>();
        Map<SkillId, List<SkillId>> children = new HashMap<>();
        LinkedHashSet<SkillId> all = new LinkedHashSet<>();
        for (SkillTreeLibrary.LinkDef l : links) {
            children.computeIfAbsent(l.parent, k -> new ArrayList<>()).add(l.child);
            parents.computeIfAbsent(l.child, k -> new ArrayList<>()).add(l.parent);
            all.add(l.parent);
            all.add(l.child);
        }
        for (SkillTreeLibrary.NodeDef def : SkillTreeLibrary.nodes())
            all.add(def.id);
        all.add(root);
        // 稳定顺序
        for (var e : children.values())
            e.sort(Comparator.comparingInt(Enum::ordinal));
        for (var e : parents.values())
            e.sort(Comparator.comparingInt(Enum::ordinal));

        // 2) 分层：level[root]=0; child 的层 = max(parent)+1
        Map<SkillId, Integer> level = new HashMap<>();
        level.put(root, 0);
        ArrayDeque<SkillId> q = new ArrayDeque<>();
        q.add(root);
        while (!q.isEmpty()) {
            SkillId u = q.poll();
            int lu = level.getOrDefault(u, 0);
            for (SkillId v : children.getOrDefault(u, List.of())) {
                int nv = Math.max(level.getOrDefault(v, 0), lu + 1);
                if (nv != level.getOrDefault(v, -1)) {
                    level.put(v, nv);
                    q.add(v);
                }
            }
        }
        int maxLvl = level.values().stream().mapToInt(i -> i).max().orElse(0);

        // 3) 逐层布局：先放 root，再按层围绕“本层父节点”给下一层的孩子分锚点
        Map<SkillId, Point> pos = new HashMap<>();
        pos.put(root, new Point(0, 0));

        // 每层要放置的节点（便于按层收尾）
        Map<Integer, List<SkillId>> nodesByLvl = new HashMap<>();
        for (SkillId id : all) {
            int lv = level.getOrDefault(id, maxLvl + 1); // 未连到根的视作“孤立”
            nodesByLvl.computeIfAbsent(lv, k -> new ArrayList<>()).add(id);
        }
        nodesByLvl.values().forEach(list -> list.sort(Comparator.comparingInt(Enum::ordinal)));

        // anchors：child -> 若干父亲分配的锚点
        Map<SkillId, List<Point>> anchors = new HashMap<>();

        // 帮助函数：角度归一化
        java.util.function.DoubleUnaryOperator norm = a -> {
            double t = a % 360.0;
            return t < 0 ? t + 360.0 : t;
        };

        // 先处理 root 的下一层
        {
            List<SkillId> kids = children.getOrDefault(root, List.of());
            int m = kids.size();
            double span = 360.0; // 根无缺口
            double start = -span / 2.0; // 让它居中
            for (int i = 0; i < m; i++) {
                double ang = start + (i + 0.5) * (span / Math.max(1, m));
                int x = (int) Math.round(radiusStep * Math.cos(Math.toRadians(ang)));
                int y = (int) Math.round(radiusStep * Math.sin(Math.toRadians(ang)));
                anchors.computeIfAbsent(kids.get(i), k -> new ArrayList<>()).add(new Point(x, y));
            }
            // 写入一层子节点的位置（单父=根，或者即便多父，此时也只有来自根的锚点）
            for (SkillId c : kids) {
                var list = anchors.getOrDefault(c, List.of());
                if (!list.isEmpty()) {
                    int sx = 0, sy = 0;
                    for (Point p : list) {
                        sx += p.x;
                        sy += p.y;
                    }
                    pos.put(c, new Point(Math.round(sx / (float) list.size()),
                            Math.round(sy / (float) list.size())));
                }
            }
            anchors.clear();
        }

        // 再处理其余层
        for (int lv = 1; lv <= maxLvl; lv++) {
            // 3.1 对本层每个父节点，给“下一层”的孩子分配锚点（围绕这个父）
            for (SkillId p : nodesByLvl.getOrDefault(lv, List.of())) {
                Point P = pos.get(p);
                if (P == null)
                    continue; // 可能是孤立分支里的点，先跳过

                // 只考虑“真正属于下一层”的孩子（避免把跨层/回边算进来）
                List<SkillId> rawKids = children.getOrDefault(p, List.of());
                List<SkillId> kids = new ArrayList<>();
                for (SkillId c : rawKids) {
                    if (level.getOrDefault(c, -1) == lv + 1)
                        kids.add(c);
                }
                if (kids.isEmpty())
                    continue;

                // 计算父节点的“朝外方向”（以便留缺口避免朝回）
                double incomingAng; // 从父的“父集合”质心 指向 父 的角度
                var pars = parents.getOrDefault(p, List.of());
                if (!pars.isEmpty()) {
                    double cx = 0, cy = 0;
                    int cnt = 0;
                    for (SkillId gp : pars) {
                        Point G = pos.get(gp);
                        if (G != null) {
                            cx += G.x;
                            cy += G.y;
                            cnt++;
                        }
                    }
                    if (cnt == 0) {
                        incomingAng = Math.toDegrees(Math.atan2(P.y, P.x)); // 退化：用从原点到父的角度
                    } else {
                        cx /= cnt;
                        cy /= cnt;
                        incomingAng = Math.toDegrees(Math.atan2(P.y - cy, P.x - cx));
                    }
                } else {
                    incomingAng = Math.toDegrees(Math.atan2(P.y, P.x)); // 没有父：沿半径
                }

                double span = Math.max(0.0, 360.0 - parentGapDeg);
                double start = norm.applyAsDouble(incomingAng - span / 2.0);

                int m = kids.size();
                for (int i = 0; i < m; i++) {
                    double ang = norm.applyAsDouble(start + (i + 0.5) * (span / m));
                    int x = P.x + (int) Math.round(radiusStep * Math.cos(Math.toRadians(ang)));
                    int y = P.y + (int) Math.round(radiusStep * Math.sin(Math.toRadians(ang)));
                    anchors.computeIfAbsent(kids.get(i), k -> new ArrayList<>()).add(new Point(x, y));
                }
            }

            // 3.2 汇总锚点：多父取平均，得到“下一层”的最终坐标
            for (SkillId c : nodesByLvl.getOrDefault(lv + 1, List.of())) {
                if (pos.containsKey(c))
                    continue; // 这一层可能已在更早步骤定过（极少见）
                var list = anchors.get(c);
                if (list == null || list.isEmpty())
                    continue; // 没拿到锚点就暂时跳过
                int sx = 0, sy = 0;
                for (Point a : list) {
                    sx += a.x;
                    sy += a.y;
                }
                pos.put(c, new Point(Math.round(sx / (float) list.size()),
                        Math.round(sy / (float) list.size())));
            }
            anchors.clear();
        }

        // 4) 还有没被连到 root 的“孤立节点”：放最外圈（仅在确有其事时）
        List<SkillId> lonely = new ArrayList<>();
        for (SkillId id : all)
            if (!pos.containsKey(id))
                lonely.add(id);
        lonely.sort(Comparator.comparingInt(Enum::ordinal));
        int n = lonely.size();
        if (n > 0) {
            int ring = Math.max(1, maxLvl + 1);
            for (int i = 0; i < n; i++) {
                double ang = (i + 0.5) * (360.0 / n);
                int x = (int) Math.round(ring * radiusStep * Math.cos(Math.toRadians(ang)));
                int y = (int) Math.round(ring * radiusStep * Math.sin(Math.toRadians(ang)));
                pos.put(lonely.get(i), new Point(x, y));
            }
        }

        return pos;
    }
}
