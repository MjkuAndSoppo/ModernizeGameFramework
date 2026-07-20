package com.modernizegameframework.bodypart;

import com.modernizegameframework.Config;
import com.modernizegameframework.ModernizeGameFramework;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 肢节血量 HUD 渲染器
 * 在副手栏上方、护甲值左侧显示人体图，按部位血量平滑着色
 * 左侧显示出血/疼痛/止痛药状态图标
 */
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BodyPartHudRenderer {

    /**
     * 单个部位的纹理与绘制信息
     */
    private record PartTexture(ResourceLocation texture, int u, int v, int texW, int texH, int drawW, int drawH) {
    }

    /**
     * 状态图标尺寸（与原版药水图标一致 18×18）
     */
    private static final int STATUS_SIZE = 18;

    /**
     * 状态图标间距
     */
    private static final int STATUS_GAP = 2;

    /**
     * 部位之间的间隙
     */
    private static final int PART_GAP = 0;

    /**
     * 纹理画布总尺寸
     */
    private static final int TEXTURE_SIZE = 128;

    /**
     * 出血状态图标
     */
    private static final ResourceLocation ICON_BLEEDING = ResourceLocation.fromNamespaceAndPath(ModernizeGameFramework.MODID, "textures/gui/bodypart/bleeding.png");

    /**
     * 调试开关：true 时用纯色块绘制，便于定位 HUD 位置
     */
    private static final boolean DEBUG_SOLID_PARTS = false;

    /**
     * 统一缩放因子：0.6 倍，整体宽度约 29 像素，匹配副手栏
     */
    private static final float SCALE = 0.6f;

    private static final PartTexture HEAD = new PartTexture(texture("head"), 51, 8, 23, 23, scaled(23), scaled(23));
    private static final PartTexture BODY = new PartTexture(texture("body"), 50, 31, 25, 43, scaled(25), scaled(43));
    private static final PartTexture LEFT_ARM = new PartTexture(texture("right_arm"), 38, 31, 12, 43, scaled(12), scaled(43));
    private static final PartTexture RIGHT_ARM = new PartTexture(texture("left_arm"), 75, 31, 12, 43, scaled(12), scaled(43));
    private static final PartTexture LEFT_LEG = new PartTexture(texture("right_leg"), 45, 71, 17, 46, scaled(17), scaled(46));
    private static final PartTexture RIGHT_LEG = new PartTexture(texture("left_leg"), 62, 71, 18, 46, scaled(18), scaled(46));

    private static int scaled(int size) {
        return Math.max(1, Math.round(size * SCALE));
    }

    private BodyPartHudRenderer() {
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(ModernizeGameFramework.MODID, "textures/gui/bodypart/" + name + ".png");
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!Config.BODYPART_ENABLED.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;

        Player player = mc.player;
        if (player == null) return;

        BodyPartHelper.getBodyPartCapability(player).ifPresent(cap -> {
            GuiGraphics graphics = event.getGuiGraphics();
            int screenWidth = event.getWindow().getGuiScaledWidth();
            int screenHeight = event.getWindow().getGuiScaledHeight();

            // 人体图整体尺寸
            int mapWidth = LEFT_ARM.drawW() + PART_GAP + BODY.drawW() + PART_GAP + RIGHT_ARM.drawW();
            int mapHeight = HEAD.drawH() + PART_GAP + BODY.drawH() + PART_GAP + LEFT_LEG.drawH();

            // 副手栏位置：hotbar 中心左侧，宽 29，高 22
            int hotbarCenterX = screenWidth / 2;
            int offhandLeftX = hotbarCenterX - 91 - 29;
            int offhandTopY = screenHeight - 23;

            // 人体图居中位于副手栏上方
            int mapOriginX = offhandLeftX + (29 - mapWidth) / 2;
            int mapOriginY = offhandTopY - 2 - mapHeight;

            // 状态图标在人体图左侧
            renderStatusIcons(graphics, cap, player, mapOriginX - STATUS_GAP - STATUS_SIZE, mapOriginY);

            // 计算人体中心
            int centerX = mapOriginX + mapWidth / 2;

            // 头部
            int headX = centerX - HEAD.drawW() / 2;
            int headY = mapOriginY;
            renderPart(graphics, HEAD, headX, headY, cap, BodyPartType.HEAD);

            // 躯干
            int bodyX = centerX - BODY.drawW() / 2;
            int bodyY = headY + HEAD.drawH() + PART_GAP;
            renderPart(graphics, BODY, bodyX, bodyY, cap, BodyPartType.BODY);

            // 手臂
            int armY = bodyY;
            int leftArmX = bodyX - LEFT_ARM.drawW() - PART_GAP;
            int rightArmX = bodyX + BODY.drawW() + PART_GAP;
            renderPart(graphics, LEFT_ARM, leftArmX, armY, cap, BodyPartType.LEFT_ARM);
            renderPart(graphics, RIGHT_ARM, rightArmX, armY, cap, BodyPartType.RIGHT_ARM);

            // 腿
            int legY = bodyY + BODY.drawH() + PART_GAP;
            int leftLegX = centerX - LEFT_LEG.drawW() - PART_GAP / 2;
            int rightLegX = centerX + PART_GAP / 2;
            renderPart(graphics, LEFT_LEG, leftLegX, legY, cap, BodyPartType.LEFT_LEG);
            renderPart(graphics, RIGHT_LEG, rightLegX, legY, cap, BodyPartType.RIGHT_LEG);
        });
    }

    /**
     * 屏蔽原版红心渲染
     * 开启肢节血量系统时隐藏原版血量条
     */
    @SubscribeEvent
    public static void onRenderOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (!Config.BODYPART_ENABLED.get()) return;
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) return;
        event.setCanceled(true);
    }

    /**
     * 在原版红心位置渲染肢节总血量分数
     * 格式：<当前剩余血量/总血量上限>
     */
    @SubscribeEvent
    public static void onRenderHealthText(RenderGuiEvent.Post event) {
        if (!Config.BODYPART_ENABLED.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;

        Player player = mc.player;
        if (player == null) return;

        BodyPartHelper.getBodyPartCapability(player).ifPresent(cap -> {
            GuiGraphics graphics = event.getGuiGraphics();
            int screenWidth = event.getWindow().getGuiScaledWidth();
            int screenHeight = event.getWindow().getGuiScaledHeight();

            int current = Math.round(cap.getTotalHealth());
            int max = Math.round(cap.getTotalMaxHealth());
            String text = current + "/" + max;

            int x = screenWidth / 2 - 91;
            // 位于原版护甲图标上方
            int y = screenHeight - 59;

            graphics.drawString(mc.font, text, x, y, 0xFFFFFF, true);
        });
    }

    /**
     * 渲染单个部位图标并按血量着色
     * 调试模式下绘制纯色块以确认位置和尺寸
     */
    private static void renderPart(GuiGraphics graphics, PartTexture part, int x, int y,
                                   BodyPartCapability cap, BodyPartType type) {
        float health = cap.getHealth(type);
        float max = cap.getMaxHealth(type);
        float percent = max > 0 ? health / max : 0.0f;

        int color;
        if (cap.isDestroyed(type)) {
            color = 0xFF000000;
        } else {
            int red = (int) Math.round(255 * (1.0f - percent));
            int green = (int) Math.round(255 * percent);
            color = 0xFF000000 | (red << 16) | (green << 8);
        }

        if (DEBUG_SOLID_PARTS) {
            // 纯色块填充
            graphics.fill(x, y, x + part.drawW(), y + part.drawH(), color);
            // 白色边框，方便看清边界
            graphics.renderOutline(x, y, part.drawW(), part.drawH(), 0xFFFFFFFF);
        } else {
            float r = (color >> 16 & 0xFF) / 255.0f;
            float g = (color >> 8 & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;

            // 使用 PoseStack 缩放，确保完整截取纹理内容并按比例绘制到屏幕
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0.0f);
            graphics.pose().scale(part.drawW() / (float) part.texW(), part.drawH() / (float) part.texH(), 1.0f);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(r, g, b, 1.0f);

            graphics.blit(part.texture(), 0, 0, part.u(), part.v(), part.texW(), part.texH(), TEXTURE_SIZE, TEXTURE_SIZE);

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();

            graphics.pose().popPose();
        }
    }

    /**
     * 渲染左侧状态图标
     * 出血/疼痛/止痛药均使用对应贴图
     */
    private static void renderStatusIcons(GuiGraphics graphics, BodyPartCapability cap, Player player, int x, int y) {
        boolean bleeding = false;
        for (BodyPartType type : BodyPartType.values()) {
            if (cap.getBleedingTicks(type) > 0) {
                bleeding = true;
                break;
            }
        }
        boolean pain = player.hasEffect(BodyPartEffects.PAIN.get());
        boolean painkiller = player.hasEffect(BodyPartEffects.PAINKILLER.get());

        int iconY = y;
        if (bleeding) {
            renderIcon(graphics, ICON_BLEEDING, x, iconY);
            iconY += STATUS_SIZE + STATUS_GAP;
        }
        if (pain) {
            renderEffectIcon(graphics, BodyPartEffects.PAIN.get(), x, iconY);
            iconY += STATUS_SIZE + STATUS_GAP;
        }
        if (painkiller) {
            renderEffectIcon(graphics, BodyPartEffects.PAINKILLER.get(), x, iconY);
        }
    }

    /**
     * 渲染原版药水/效果图标
     */
    private static void renderEffectIcon(GuiGraphics graphics, MobEffect effect, int x, int y) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getMobEffectTextures().get(effect);
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        graphics.blit(x, y, 0, STATUS_SIZE, STATUS_SIZE, sprite);
    }

    /**
     * 渲染独立图标贴图
     */
    private static void renderIcon(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(texture, x, y, 0, 0, STATUS_SIZE, STATUS_SIZE, STATUS_SIZE, STATUS_SIZE);
        RenderSystem.disableBlend();
    }
}
