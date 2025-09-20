package com.cceve.passivesystemskill.client;

import java.util.*;
import com.cceve.passivesystemskill.capability.PlayerVariables.PlayerSkills;
import com.cceve.passivesystemskill.capability.PlayerVariables.SkillId;
import com.cceve.passivesystemskill.capability.PlayerVariablesProvider;
import com.cceve.passivesystemskill.network.ModNetwork;
import com.cceve.passivesystemskill.pss_main;
import com.cceve.passivesystemskill.skilltree.AutoLayout;
import com.cceve.passivesystemskill.skilltree.SkillTreeLibrary;
import com.cceve.passivesystemskill.skilltree.SkillTreeLibrary.LinkDef;
import com.cceve.passivesystemskill.skilltree.SkillTreeLibrary.NodeDef;
import com.cceve.passivesystemskill.skilltree.SkillTreeLibrary.Quality;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class SkillTreeScreen extends Screen {

    /* ====== “树枝效果”&防重叠参数 ====== */
    private static final double TARGET_LEN_BASE = 250.0; // 根->第一层 目标长度
    private static final double TARGET_LEN_DECAY = 0.3; // 每层递减系数 (0.75~0.92)
    private static final double TARGET_LEN_MIN = 50.0;
    private static final double TARGET_LEN_MAX = 400.0;

    // 同父子节点之间希望的最小屏幕像素间距，换算成角度（≈ SPACING_PX / r）
    private static final double SIBLING_SPACING_PX = 30.0;

    // 标签绘制：沿径向外移的像素
    private static final int LABEL_OFFSET_PX = 18;
    private static final float LABEL_BG_ALPHA = 0.35f;
    private static final double SHOW_NAME_MIN_SCALE = 0.85; // 放大到这个倍数才显示名字
    private static final double SHOW_LEVEL_MIN_SCALE = 0.85; // 放大到这个倍数才显示等级

    /* ====== 节点渲染用结构 ====== */
    private static class Node {
        int x, y; // 世界坐标（会被“枝丫压缩/兄弟角度均分”修改）
        String name;
        SkillId skillId;
        int costPerLevel, maxLevel;
        ResourceLocation iconTex;
        ItemStack iconItem;
        Quality quality;

        Node(int x, int y, String name, SkillId skillId,
                int costPerLevel, int maxLevel,
                ResourceLocation iconTex, net.minecraft.world.item.Item iconItem,
                Quality quality) {
            this.x = x;
            this.y = y;
            this.name = name;
            this.skillId = skillId;
            this.costPerLevel = costPerLevel;
            this.maxLevel = maxLevel;
            this.iconTex = iconTex;
            this.iconItem = (iconItem == null) ? ItemStack.EMPTY : new ItemStack(iconItem);
            this.quality = quality;
        }
    }

    private final List<Node> nodes = new ArrayList<>();
    private final List<Node[]> links = new ArrayList<>();
    private final Map<SkillId, Node> id2node = new HashMap<>();
    private final Map<SkillId, Integer> depth = new HashMap<>();
    private final Map<SkillId, List<SkillId>> children = new HashMap<>();
    private final Map<SkillId, SkillId> parent = new HashMap<>();

    /* 视图状态 */
    private double offsetX = 0, offsetY = 0, scale = 1.0;
    private static final double MIN_SCALE = 0.06, MAX_SCALE = 3.0;
    private static final double SCROLL_STEP_NORMAL = 1.12, SCROLL_STEP_FINE = 1.04, SCROLL_STEP_FAST = 1.25;
    private boolean dragging = false;
    private int lastMouseX, lastMouseY;

    public SkillTreeScreen() {
        super(Component.translatable("screen.passivesystemskill.skill_tree"));
    }

    @Override
    protected void init() {
        super.init();
        nodes.clear();
        links.clear();
        id2node.clear();
        depth.clear();
        children.clear();
        parent.clear();
        offsetX = this.width / 2.0;
        offsetY = this.height / 2.0;
        scale = 1.0;

        SkillTreeLibrary.bootstrap();

        // 1) 先用 AutoLayout 得到初始“方向”
        var posMap = AutoLayout.compute(SkillTreeLibrary.root(), SkillTreeLibrary.links(), 160, 100.0);

        for (NodeDef def : SkillTreeLibrary.nodes()) {
            var pt = posMap.get(def.id);
            int x = (pt != null ? pt.x : 0);
            int y = (pt != null ? pt.y : 0);
            Node n = new Node(x, y, def.name, def.id, def.costPerLevel, def.maxLevel,
                    def.iconTex, def.iconItem == null ? null : def.iconItem.get().asItem(), def.quality);
            nodes.add(n);
            id2node.put(def.id, n);
        }
        for (LinkDef l : SkillTreeLibrary.links()) {
            Node p = id2node.get(l.parent);
            Node c = id2node.get(l.child);
            if (p != null && c != null) {
                links.add(new Node[] { p, c });
                children.computeIfAbsent(l.parent, k -> new ArrayList<>()).add(l.child);
                parent.putIfAbsent(l.child, l.parent);
            }
        }

        // 2) 深度 + “枝丫压缩” + “兄弟角度均分/最小角间距”
        computeDepth();
        compressIntoBranches(); // 控制父→子长度
        spreadSiblingsAngular(); // 控制同父子节点角度间距

        if (Minecraft.getInstance().player != null) {
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.C2S_RequestFullSync());
        }
    }

    /* ================= 核心一：枝丫压缩（让连线越来越短） ================= */
    private void computeDepth() {
        SkillId r = SkillTreeLibrary.root();
        if (r == null)
            return;
        ArrayDeque<SkillId> q = new ArrayDeque<>();
        depth.put(r, 0);
        q.add(r);
        while (!q.isEmpty()) {
            SkillId u = q.poll();
            int du = depth.getOrDefault(u, 0);
            for (SkillId v : children.getOrDefault(u, Collections.emptyList())) {
                if (!depth.containsKey(v)) {
                    depth.put(v, du + 1);
                    q.add(v);
                }
            }
        }
    }

    private void compressIntoBranches() {
        SkillId root = SkillTreeLibrary.root();
        if (root == null || !id2node.containsKey(root))
            return;

        List<SkillId> order = new ArrayList<>(depth.keySet());
        order.sort(Comparator.comparingInt(depth::get));

        for (SkillId v : order) {
            Integer dv = depth.get(v);
            if (dv == null || dv == 0)
                continue;
            SkillId pv = parent.get(v);
            if (pv == null)
                continue;
            Node p = id2node.get(pv);
            Node c = id2node.get(v);
            if (p == null || c == null)
                continue;

            double dx = c.x - p.x, dy = c.y - p.y;
            double len = Math.hypot(dx, dy);
            if (len < 1e-6) {
                dx = 1;
                dy = 0;
                len = 1;
            }

            double target = TARGET_LEN_BASE * Math.pow(TARGET_LEN_DECAY, Math.max(0, dv - 1));
            target = Math.max(TARGET_LEN_MIN, Math.min(TARGET_LEN_MAX, target));

            double ux = dx / len, uy = dy / len;
            c.x = (int) Math.round(p.x + ux * target);
            c.y = (int) Math.round(p.y + uy * target);
        }
    }

    /*
     * ================= 核心二：同父兄弟角度均分 + 最小角间距 =================
     * 思路：保持每个子节点到父的半径不变，只调整“角度”，
     * 让相邻孩子至少相差 dθ ≈ SIBLING_SPACING_PX / r；再整体回中。
     */
    private void spreadSiblingsAngular() {
        for (var e : children.entrySet()) {
            SkillId pid = e.getKey();
            Node p = id2node.get(pid);
            if (p == null)
                continue;

            List<SkillId> ch = e.getValue();
            if (ch == null || ch.size() <= 1)
                continue;

            // 计算孩子的极角和半径
            class ChildPolar {
                SkillId id;
                Node n;
                double r, ang;
                double origAng;

                ChildPolar(SkillId id, Node n, double r, double ang) {
                    this.id = id;
                    this.n = n;
                    this.r = r;
                    this.ang = ang;
                    this.origAng = ang;
                }
            }
            List<ChildPolar> polars = new ArrayList<>(ch.size());
            for (SkillId cid : ch) {
                Node c = id2node.get(cid);
                if (c == null)
                    continue;
                double dx = c.x - p.x, dy = c.y - p.y;
                double r = Math.max(1e-6, Math.hypot(dx, dy));
                double ang = Math.atan2(dy, dx); // [-π, π]
                polars.add(new ChildPolar(cid, c, r, ang));
            }
            if (polars.size() <= 1)
                continue;

            // 按角排序
            polars.sort(Comparator.comparingDouble(a -> a.ang));

            // 最小角间距（不同半径可能不同，取相邻两点用较大的 dθ）
            // dθ_i = SIBLING_SPACING_PX / r_i （小角度近似）
            double[] minGap = new double[polars.size()];
            for (int i = 0; i < polars.size(); i++) {
                minGap[i] = Math.min(1.2, SIBLING_SPACING_PX / Math.max(8.0, polars.get(i).r));
            }
            // 单向扫描把太近的点推开
            for (int i = 1; i < polars.size(); i++) {
                double need = Math.max(minGap[i - 1], minGap[i]);
                double prev = polars.get(i - 1).ang;
                double cur = polars.get(i).ang;
                if (cur - prev < need) {
                    polars.get(i).ang = prev + need;
                }
            }
            // 如果尾首又太近，整体加上一个旋转量
            double needWrap = Math.max(minGap[0], minGap[polars.size() - 1]);
            double totalArc = polars.get(polars.size() - 1).ang - polars.get(0).ang;
            if (totalArc + needWrap > Math.PI * 2) {
                // 按超出的量等比分摊
                double overflow = totalArc + needWrap - Math.PI * 2;
                double per = overflow / (polars.size() - 1);
                for (int i = 1; i < polars.size(); i++)
                    polars.get(i).ang -= per * i;
            }

            // 回中：保持与原角度均值一致，避免整团偏移
            double meanOrig = 0, meanNew = 0;
            for (ChildPolar cp : polars) {
                meanOrig += cp.origAng;
                meanNew += cp.ang;
            }
            double delta = (meanOrig - meanNew) / polars.size();
            for (ChildPolar cp : polars)
                cp.ang += delta;

            // 写回坐标（半径不变，只换角度）
            for (ChildPolar cp : polars) {
                cp.n.x = (int) Math.round(p.x + cp.r * Math.cos(cp.ang));
                cp.n.y = (int) Math.round(p.y + cp.r * Math.sin(cp.ang));
            }
        }
    }

    /* ================= 渲染 ================= */
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(g);
        g.drawCenteredString(this.font,
                "技能树：左键升级;Shift+左键一次+10级;滚轮缩放;拖拽移动;F=全图;0=重置",
                this.width / 2, 12, 0xFFFFCC00);

        var mc = Minecraft.getInstance();
        PlayerSkills skillMap = mc.player == null ? null
                : mc.player.getCapability(PlayerVariablesProvider.CAPABILITY).map(v -> v.skillMap).orElse(null);
        int skillPoints = (int) (mc.player == null ? 0.0
                : mc.player.getCapability(PlayerVariablesProvider.CAPABILITY)
                        .map(v -> v.currencies.skillPoints).orElse(0.0));

        String spText = "技能点: " + skillPoints;
        g.drawString(this.font, spText, this.width - this.font.width(spText) - 8, 12, 0xFF66FF66, false);

        int worldMX = screenToWorldX(mouseX), worldMY = screenToWorldY(mouseY);

        // 边（点状直线）
        for (Node[] pair : links) {
            Node a = pair[0], b = pair[1];
            boolean aUnlocked = isUnlocked(skillMap, a.skillId);
            boolean bUnlocked = isUnlocked(skillMap, b.skillId);
            int color = (aUnlocked && bUnlocked) ? 0xFF33FF66 : 0xFF888888;
            drawEdgeSimple(g, a, b, color);
        }

        // 节点 + 标签（标签沿径向外移，避免压在图标上）
        Node hovered = null;
        for (Node n : nodes) {
            boolean isHover = isInside(n, worldMX, worldMY);
            if (isHover)
                hovered = n;

            int sx = worldToScreenX(n.x), sy = worldToScreenY(n.y);
            int frameSize = (int) Math.max(10, Math.round(26 * scale));
            int half = frameSize / 2;

            int lv = levelOf(skillMap, n.skillId);
            boolean unlocked = lv > 0;

            ResourceLocation base = getFrameBaseTex(n.quality);
            int fallback = (base.getPath().contains("blue") ? 0xFF3A66FF : 0xFF33CC66);
            drawFrameStatefulSafe(g, base, sx - half, sy - half, frameSize, frameSize, unlocked, isHover, fallback);

            int iconSize = Math.max(8, (int) Math.round(16 * scale));
            int ix = sx - iconSize / 2, iy = sy - iconSize / 2;

            if (n.iconTex != null)
                drawTexture(g, n.iconTex, ix, iy, iconSize, iconSize);
            else if (!n.iconItem.isEmpty())
                renderItemCenteredScaled(g, n.iconItem, sx, sy, iconSize);
            else
                g.fill(ix, iy, ix + iconSize, iy + iconSize, unlocked ? 0x8833FF66 : 0x88AA00FF);

            // —— 标签沿径向外移 —— //
            boolean showName = scale >= SHOW_NAME_MIN_SCALE;
            boolean showLevel = scale >= SHOW_LEVEL_MIN_SCALE;
            if (showName || showLevel) {
                // 径向方向：优先用父->子向量；没有父就用根
                int px = sx, py = sy;
                SkillId pid = parent.get(n.skillId);
                if (pid == null) {
                    SkillId root = SkillTreeLibrary.root();
                    Node r = root == null ? null : id2node.get(root);
                    if (r != null) {
                        px = worldToScreenX(r.x);
                        py = worldToScreenY(r.y);
                    }
                } else {
                    Node p = id2node.get(pid);
                    if (p != null) {
                        px = worldToScreenX(p.x);
                        py = worldToScreenY(p.y);
                    }
                }
                double vx = sx - px, vy = sy - py;
                double L = Math.max(1e-6, Math.hypot(vx, vy));
                double ux = vx / L, uy = vy / L;
                int tx = (int) Math.round(sx + ux * (LABEL_OFFSET_PX + half));
                int ty = (int) Math.round(sy + uy * (LABEL_OFFSET_PX + half));

                // 背景 + 文本
                int yCursor = ty - (showLevel ? this.font.lineHeight : 0);
                if (showName) {
                    drawLabelWithBG(g, n.name, tx, yCursor, 0xFFFFFFFF);
                    yCursor += this.font.lineHeight + 1;
                }
                if (showLevel) {
                    String lvl = "等级: " + lv + "/" + n.maxLevel;
                    drawLabelWithBG(g, lvl, tx, yCursor, 0xFFEEEEAA);
                }
            }
        }

        // 悬浮提示（循环外、只显示一个）——放在 super.render 之后，这样一定在最上层
        if (hovered != null) {
            int hv = levelOf(skillMap, hovered.skillId);
            boolean parentsOK = parentsSatisfiedClient(hovered.skillId, skillMap); // ← 两参版本

            List<Component> tips = new ArrayList<>();
            tips.add(Component.literal(hovered.name).withStyle(ChatFormatting.LIGHT_PURPLE));
            tips.add(Component.literal("等级: " + hv + "/" + hovered.maxLevel).withStyle(ChatFormatting.YELLOW));
            tips.add(Component.literal("每级花费: " + hovered.costPerLevel).withStyle(ChatFormatting.GRAY));

            // ★★ 新增：经验 now/next（仅未满级时显示） ★★
            if (hovered.maxLevel >= 100 && hv < hovered.maxLevel) {
                var rec = (skillMap != null) ? skillMap.get(hovered.skillId) : null;
                int now = (rec != null ? rec.expNow : 0);
                int next = (rec != null && rec.expNext > 0 ? rec.expNext : 5); // rec 为空时兜底 5
                ChatFormatting col = (now >= next) ? ChatFormatting.GREEN : ChatFormatting.AQUA;
                tips.add(Component.literal("经验: " + now + " / " + next).withStyle(col));
            }

            var def = SkillTreeLibrary.node(hovered.skillId);
            if (def != null) {
                tips.add(Component.empty());
                if (def.dynamicDesc != null) {
                    tips.addAll(def.dynamicDesc.apply(hv)); // hv = 当前等级
                } else if (!def.description.isEmpty()) {
                    tips.addAll(def.description);
                }
            }

            if (!parentsOK) {
                tips.add(Component.literal("前置未满足：").withStyle(ChatFormatting.RED));
                for (SkillId par : SkillTreeLibrary.parentsOf(hovered.skillId)) {
                    int need = SkillTreeLibrary.requiredParentLevel(par, hovered.skillId);
                    int have = levelOf(skillMap, par);
                    var pdef = SkillTreeLibrary.node(par);
                    String pname = (pdef != null ? pdef.name : par.name());
                    tips.add(Component.literal("- " + pname + " ≥ " + need + "（当前 " + have + "）")
                            .withStyle(have >= need ? ChatFormatting.GREEN : ChatFormatting.RED));
                }
            }

            g.renderComponentTooltip(this.font, tips, mouseX, mouseY);
        }

        // ……for (Node n : nodes) { ... } 循环结束这里插入
        super.render(g, mouseX, mouseY, partialTicks);

    }

    /* ====== 标签带半透明底（提升可读性且不抢占空间） ====== */
    private void drawLabelWithBG(GuiGraphics g, String text, int cx, int cy, int colorARGB) {
        int w = this.font.width(text), h = this.font.lineHeight;
        int x = cx - w / 2, y = cy - h / 2;
        // 半透明底
        int a = (int) (LABEL_BG_ALPHA * 255) & 0xFF;
        int bg = (a << 24) | 0x000000; // 黑底
        g.fill(x - 2, y - 1, x + w + 2, y + h + 1, bg);
        g.drawString(this.font, text, x, y, colorARGB, false);
    }

    /* ====== 绘边（点状直线） ====== */
    private void drawEdgeSimple(GuiGraphics g, Node a, Node b, int color) {
        int x1 = worldToScreenX(a.x), y1 = worldToScreenY(a.y);
        int x2 = worldToScreenX(b.x), y2 = worldToScreenY(b.y);
        drawLine(g, x1, y1, x2, y2, color);
    }

    /* ====== 输入 ====== */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Node hit = getNodeAtScreen((int) mouseX, (int) mouseY);
            if (hit != null) {
                onClickNode(hit);
                return true;
            }
            dragging = true;
            lastMouseX = (int) mouseX;
            lastMouseY = (int) mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            int dx = (int) mouseX - lastMouseX, dy = (int) mouseY - lastMouseY;
            offsetX += dx;
            offsetY += dy;
            lastMouseX = (int) mouseX;
            lastMouseY = (int) mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta == 0)
            return false;
        double step = hasControlDown() ? SCROLL_STEP_FAST : (hasShiftDown() ? SCROLL_STEP_FINE : SCROLL_STEP_NORMAL);
        double old = scale, next = (delta > 0) ? scale * step : scale / step;
        next = Math.max(MIN_SCALE, Math.min(MAX_SCALE, next));
        if (Math.abs(next - old) < 1e-9)
            return true;
        double mx = mouseX, my = mouseY, worldX = (mx - offsetX) / old, worldY = (my - offsetY) / old;
        scale = next;
        offsetX = mx - worldX * scale;
        offsetY = my - worldY * scale;
        return true;
    }

    // --- ② 点击逻辑：把原来的 onClickNode(...) 整个替换为下面版本 ---
    /* 点击：左键升级；Shift+左键一次+10级；解锁(0→1)前要满足父要求 */
    private void onClickNode(Node node) {
        var mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        int times = hasShiftDown() ? 10 : 1;

        // 客户端快速门槛：仅在从 0 级尝试解锁时检查父需求
        PlayerSkills skillMap = mc.player.getCapability(PlayerVariablesProvider.CAPABILITY)
                .map(v -> v.skillMap).orElse(null);
        int curLv = levelOf(skillMap, node.skillId);
        boolean unlocking = (curLv <= 0); // 第一次升级会解锁

        if (unlocking && !parentsSatisfiedClient(node.skillId, skillMap)) {
            mc.player.displayClientMessage(Component.literal("前置未满足：请先提升父节点到要求等级").withStyle(ChatFormatting.RED), true);
            return;
        }

        // 发送升级请求（一次或10次；服务器仍会做权威校验）
        for (int i = 0; i < times; i++) {
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.C2S_ToggleNode(node.skillId, true, 0));
        }
    }

    /* ====== 工具 ====== */
    private int levelOf(PlayerSkills map, SkillId id) {
        if (map == null || id == null)
            return 0;
        var rec = map.get(id);
        return rec == null ? 0 : rec.level;
    }

    private boolean isUnlocked(PlayerSkills map, SkillId id) {
        return levelOf(map, id) > 0;
    }

    private Node findById(SkillId id) {
        for (Node n : nodes)
            if (n.skillId == id)
                return n;
        return null;
    }

    private Node getNodeAtScreen(int sx, int sy) {
        int wx = screenToWorldX(sx), wy = screenToWorldY(sy);
        for (int i = nodes.size() - 1; i >= 0; i--) {
            Node n = nodes.get(i);
            if (isInside(n, wx, wy))
                return n;
        }
        return null;
    }

    private boolean isInside(Node n, int wx, int wy) {
        int dx = wx - n.x, dy = wy - n.y;
        int r = Math.max(6, (int) Math.round(12 * scale));
        return dx * dx + dy * dy <= r * r;
    }

    private int worldToScreenX(int wx) {
        return (int) Math.round(wx * scale + offsetX);
    }

    private int worldToScreenY(int wy) {
        return (int) Math.round(wy * scale + offsetY);
    }

    private int screenToWorldX(int sx) {
        return (int) Math.round((sx - offsetX) / scale);
    }

    private int screenToWorldY(int sy) {
        return (int) Math.round((sy - offsetY) / scale);
    }

    private void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1, dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) {
            g.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        int jump = Math.max(1, (int) Math.round(1.0 / Math.max(0.0001, scale)));
        float sx = dx / (float) steps, sy = dy / (float) steps;
        float x = x1, y = y1;
        for (int i = 0; i <= steps; i += jump) {
            int ix = Math.round(x), iy = Math.round(y);
            g.fill(ix, iy, ix + 1, iy + 1, color);
            x += sx * jump;
            y += sy * jump;
        }
    }

    private void drawTexture(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h) {
        RenderSystem.setShaderTexture(0, tex);
        g.blit(tex, x, y, 0, 0, w, h, w, h);
    }

    private void renderItemCenteredScaled(GuiGraphics g, ItemStack stack, int cx, int cy, int sizePx) {
        if (stack == null || stack.isEmpty())
            return;
        float s = sizePx / 16f;
        RenderSystem.enableDepthTest();
        g.pose().pushPose();
        g.pose().translate(cx - sizePx / 2f, cy - sizePx / 2f, 200);
        g.pose().scale(s, s, 1f);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
    }

    private void drawFrameStateful(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, boolean unlocked,
            boolean hover) {
        RenderSystem.enableBlend();
        float mul = unlocked ? 1.0f : 0.6f;
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShaderColor(mul, mul, mul, 1.0f);
        g.blit(tex, x, y, 0, 0, w, h, w, h);
        if (hover) {
            RenderSystem.setShaderColor(1f, 1f, 1f, 0.35f);
            g.blit(tex, x, y, 0, 0, w, h, w, h);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private ResourceLocation getFrameBaseTex(Quality q) {
        String color = (q == null) ? "green" : q.name().toLowerCase();
        return ResourceLocation.fromNamespaceAndPath(pss_main.MODID, "textures/gui/frames/" + color + ".png");
    }

    // 客户端前置校验：父节点需达到各自 requiredLevel（仅用于显示/点击门槛）
    private boolean parentsSatisfiedClient(SkillId childId, PlayerSkills map) {
        if (map == null)
            return false;
        for (SkillId par : SkillTreeLibrary.parentsOf(childId)) {
            int need = SkillTreeLibrary.requiredParentLevel(par, childId);
            int have = levelOf(map, par);
            if (have < need)
                return false;
        }
        return true;
    }

    private boolean hasTexture(ResourceLocation tex) {
        try {
            return Minecraft.getInstance().getResourceManager().getResource(tex).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    private void drawFrameStatefulSafe(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h,
            boolean unlocked, boolean hover, int fallbackColorARGB) {
        if (!hasTexture(tex)) {
            g.fill(x, y, x + w, y + h, fallbackColorARGB);
            return;
        }
        drawFrameStateful(g, tex, x, y, w, h, unlocked, hover);
    }

    private static java.util.List<net.minecraft.util.FormattedCharSequence> wrapTooltip(
            Font font, java.util.List<Component> lines, int maxWidthPx) {
        java.util.List<net.minecraft.util.FormattedCharSequence> out = new java.util.ArrayList<>();
        for (var c : lines)
            out.addAll(font.split(c, maxWidthPx));
        return out;
    }

    private void fitToContent(double marginPx) {
        if (nodes.isEmpty())
            return;
        int minx = Integer.MAX_VALUE, miny = Integer.MAX_VALUE, maxx = Integer.MIN_VALUE, maxy = Integer.MIN_VALUE;
        for (Node n : nodes) {
            if (n.x < minx)
                minx = n.x;
            if (n.x > maxx)
                maxx = n.x;
            if (n.y < miny)
                miny = n.y;
            if (n.y > maxy)
                maxy = n.y;
        }
        double w = Math.max(1, maxx - minx), h = Math.max(1, maxy - miny);
        double sX = (this.width - 2 * marginPx) / w, sY = (this.height - 2 * marginPx) / h;
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, Math.min(sX, sY)));
        double cx = (minx + maxx) / 2.0, cy = (miny + maxy) / 2.0;
        offsetX = this.width / 2.0 - cx * scale;
        offsetY = this.height / 2.0 - cy * scale;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_F) {
            fitToContent(80);
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_0) {
            scale = 1.0;
            offsetX = this.width / 2.0;
            offsetY = this.height / 2.0;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
