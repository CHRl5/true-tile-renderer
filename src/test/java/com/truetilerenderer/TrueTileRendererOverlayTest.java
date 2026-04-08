package com.truetilerenderer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.HeadIcon;
import net.runelite.api.HitsplatID;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.SkullIcon;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class TrueTileRendererOverlayTest {
  private Client client;
  private TrueTileRendererPlugin plugin;
  private TrueTileRendererConfig config;
  private SpriteManager spriteManager;
  private TrueTileRendererOverlay overlay;

  @Before
  public void setUp() {
    client = Mockito.mock(Client.class);
    plugin = Mockito.mock(TrueTileRendererPlugin.class);
    config = Mockito.mock(TrueTileRendererConfig.class);
    spriteManager = Mockito.mock(SpriteManager.class);
    overlay = TestUtils.newOverlay(client, plugin, config, spriteManager);
  }

  @Test
  public void constructorSetsExpectedOverlayLayerAndPosition() {
    assertEquals(OverlayLayer.ABOVE_SCENE, overlay.getLayer());
    assertEquals(OverlayPosition.DYNAMIC, overlay.getPosition());
  }

  @Test
  public void renderInvokesPluginAndVisitsConfiguredActors() {
    Graphics2D graphics = newGraphics();
    Player player = Mockito.mock(Player.class);
    NPC npc = Mockito.mock(NPC.class);
    LocalPoint localPoint = new LocalPoint(128, 128, 0);

    when(config.renderLocalPlayer()).thenReturn(true);
    when(config.renderNpcs()).thenReturn(true);
    when(client.getLocalPlayer()).thenReturn(player);
    when(plugin.getTrackedNpcs()).thenReturn(Set.of(npc));
    when(plugin.getTrueTileLocation(player)).thenReturn(localPoint);
    when(plugin.getTrueTileLocation(npc)).thenReturn(localPoint);

    overlay.render(graphics);

    verify(plugin).renderOutlines();
    verify(plugin).getTrueTileLocation(player);
    verify(plugin).getTrueTileLocation(npc);
  }

  @Test
  public void renderActorTextReturnsEarlyForNullActorOrLocation() {
    Graphics2D graphics = newGraphics();
    Actor actor = Mockito.mock(Actor.class);

    TestUtils.invoke(
        overlay, "renderActorText", new Class<?>[] {Graphics2D.class, Actor.class}, graphics, null);
    verifyNoInteractions(plugin);

    when(plugin.getTrueTileLocation(actor)).thenReturn(null);
    TestUtils.invoke(
        overlay,
        "renderActorText",
        new Class<?>[] {Graphics2D.class, Actor.class},
        graphics,
        actor);
    verify(plugin).getTrueTileLocation(actor);
  }

  @Test
  public void renderActorTextMirrorsAllEnabledPlayerElementsAndRestoresGraphicsState() {
    Graphics2D graphics = newGraphics();
    Player player = Mockito.mock(Player.class);
    LocalPoint localPoint = new LocalPoint(128, 128, 0);
    BufferedImage skullImage = image(12, 12);
    BufferedImage prayerImage = image(10, 10);
    Font originalFont = graphics.getFont();
    Color originalColor = new Color(10, 20, 30, 40);
    Stroke originalStroke = new BasicStroke(3f);

    graphics.setColor(originalColor);
    graphics.setStroke(originalStroke);
    when(plugin.getTrueTileLocation(player)).thenReturn(localPoint);
    when(plugin.getTrackedHitsplats(player))
        .thenReturn(List.of(TestUtils.mirroredHitsplat(HitsplatID.BLOCK_OTHER, 22, 130)));
    when(config.mirrorActorNames()).thenReturn(true);
    when(config.mirrorHealthBars()).thenReturn(true);
    when(config.mirrorHeadIcons()).thenReturn(true);
    when(config.mirrorHitsplats()).thenReturn(true);
    when(config.playerOutlineColor()).thenReturn(new Color(0, 255, 255, 180));
    when(player.getName()).thenReturn("Player");
    when(player.getLogicalHeight()).thenReturn(50);
    when(player.getHealthScale()).thenReturn(20);
    when(player.getHealthRatio()).thenReturn(10);
    when(player.getSkullIcon()).thenReturn(SkullIcon.SKULL);
    when(player.getOverheadIcon()).thenReturn(HeadIcon.MELEE);
    when(client.getGameCycle()).thenReturn(100);
    when(spriteManager.getSprite(SpriteID.HeadiconsPkInterface.PLAYER_KILLER_SKULL, 0))
        .thenReturn(skullImage);
    when(spriteManager.getSprite(SpriteID.Prayeron.PROTECT_FROM_MELEE, 0)).thenReturn(prayerImage);

    try (MockedStatic<net.runelite.api.Perspective> perspective =
            Mockito.mockStatic(net.runelite.api.Perspective.class);
        MockedStatic<OverlayUtil> overlayUtil = Mockito.mockStatic(OverlayUtil.class)) {
      perspective
          .when(
              () ->
                  net.runelite.api.Perspective.getCanvasTextLocation(
                      client, graphics, localPoint, "Player", 142))
          .thenReturn(new Point(10, 10));
      perspective
          .when(
              () ->
                  net.runelite.api.Perspective.getCanvasImageLocation(
                      client, localPoint, skullImage, 102))
          .thenReturn(new Point(20, 20));
      perspective
          .when(
              () ->
                  net.runelite.api.Perspective.getCanvasImageLocation(
                      client, localPoint, prayerImage, 102))
          .thenReturn(new Point(25, 20));
      perspective
          .when(
              () ->
                  net.runelite.api.Perspective.getCanvasImageLocation(
                      client, localPoint, getHealthBarAnchor(), 84))
          .thenReturn(new Point(30, 30));
      perspective
          .when(
              () ->
                  net.runelite.api.Perspective.getCanvasTextLocation(
                      client, graphics, localPoint, "22", 122))
          .thenReturn(new Point(40, 40));

      TestUtils.invoke(
          overlay,
          "renderActorText",
          new Class<?>[] {Graphics2D.class, Actor.class},
          graphics,
          player);

      overlayUtil.verify(
          () ->
              OverlayUtil.renderTextLocation(
                  graphics, new Point(10, 10), "Player", new Color(0, 255, 255, 180)));
      overlayUtil.verify(
          () -> OverlayUtil.renderImageLocation(graphics, new Point(8, 20), skullImage));
      overlayUtil.verify(
          () -> OverlayUtil.renderImageLocation(graphics, new Point(27, 20), prayerImage));
      overlayUtil.verify(
          () ->
              OverlayUtil.renderTextLocation(
                  graphics, new Point(40, 40), "22", new Color(96, 160, 255, 240)));
    }

    assertEquals(originalFont, graphics.getFont());
    assertEquals(originalColor, graphics.getColor());
    assertEquals(originalStroke, graphics.getStroke());
  }

  @Test
  public void renderHealthBarSkipsInvalidDataAndMissingCanvasPoint() {
    Graphics2D graphics = newGraphics();
    Actor actor = Mockito.mock(Actor.class);
    LocalPoint localPoint = new LocalPoint(128, 128, 0);
    Color originalColor = graphics.getColor();
    Stroke originalStroke = graphics.getStroke();

    when(actor.getHealthScale()).thenReturn(0);
    when(actor.getHealthRatio()).thenReturn(5);
    TestUtils.invoke(
        overlay,
        "renderHealthBar",
        new Class<?>[] {Graphics2D.class, Actor.class, LocalPoint.class},
        graphics,
        actor,
        localPoint);
    assertEquals(originalColor, graphics.getColor());
    assertEquals(originalStroke, graphics.getStroke());

    when(actor.getHealthScale()).thenReturn(20);
    when(actor.getHealthRatio()).thenReturn(10);
    when(actor.getLogicalHeight()).thenReturn(30);
    try (MockedStatic<net.runelite.api.Perspective> perspective =
        Mockito.mockStatic(net.runelite.api.Perspective.class)) {
      perspective
          .when(
              () ->
                  net.runelite.api.Perspective.getCanvasImageLocation(
                      client, localPoint, getHealthBarAnchor(), 64))
          .thenReturn(null);

      TestUtils.invoke(
          overlay,
          "renderHealthBar",
          new Class<?>[] {Graphics2D.class, Actor.class, LocalPoint.class},
          graphics,
          actor,
          localPoint);
    }

    assertEquals(originalColor, graphics.getColor());
    assertEquals(originalStroke, graphics.getStroke());
  }

  @Test
  public void renderHeadIconsReturnsEarlyWhenNoIconsExist() {
    Graphics2D graphics = newGraphics();
    Actor actor = Mockito.mock(Actor.class);

    TestUtils.invoke(
        overlay,
        "renderHeadIcons",
        new Class<?>[] {Graphics2D.class, Actor.class, LocalPoint.class},
        graphics,
        actor,
        new LocalPoint(128, 128, 0));
    verifyNoInteractions(spriteManager);
  }

  @Test
  public void getHeadIconImagesHandlesNpcArchivesAndMissingSprites() {
    NPC npc = Mockito.mock(NPC.class);
    BufferedImage icon = image(8, 8);

    when(npc.getOverheadArchiveIds()).thenReturn(new int[] {10, -1});
    when(npc.getOverheadSpriteIds()).thenReturn(new short[] {1, 2});
    when(spriteManager.getSprite(10, 1)).thenReturn(icon);

    @SuppressWarnings("unchecked")
    List<BufferedImage> images =
        TestUtils.invoke(overlay, "getHeadIconImages", new Class<?>[] {Actor.class}, npc);
    assertEquals(List.of(icon), images);

    when(npc.getOverheadArchiveIds()).thenReturn(null);
    when(npc.getOverheadSpriteIds()).thenReturn(null);
    @SuppressWarnings("unchecked")
    List<BufferedImage> emptyImages =
        TestUtils.invoke(overlay, "getHeadIconImages", new Class<?>[] {Actor.class}, npc);
    assertTrue(emptyImages.isEmpty());
  }

  @Test
  public void getPrayerImageCoversMappedAndDefaultCases() {
    BufferedImage image = image(8, 8);
    when(spriteManager.getSprite(anyInt(), eq(0))).thenReturn(image);

    assertNull(
        TestUtils.invoke(
            overlay, "getPrayerImage", new Class<?>[] {HeadIcon.class}, (HeadIcon) null));
    assertSame(
        image,
        TestUtils.invoke(
            overlay, "getPrayerImage", new Class<?>[] {HeadIcon.class}, HeadIcon.MELEE));
    assertSame(
        image,
        TestUtils.invoke(
            overlay, "getPrayerImage", new Class<?>[] {HeadIcon.class}, HeadIcon.RANGED));
    assertSame(
        image,
        TestUtils.invoke(
            overlay, "getPrayerImage", new Class<?>[] {HeadIcon.class}, HeadIcon.MAGIC));
    assertSame(
        image,
        TestUtils.invoke(
            overlay, "getPrayerImage", new Class<?>[] {HeadIcon.class}, HeadIcon.RETRIBUTION));
    assertSame(
        image,
        TestUtils.invoke(
            overlay, "getPrayerImage", new Class<?>[] {HeadIcon.class}, HeadIcon.SMITE));
    assertSame(
        image,
        TestUtils.invoke(
            overlay, "getPrayerImage", new Class<?>[] {HeadIcon.class}, HeadIcon.REDEMPTION));
    assertSame(
        image,
        TestUtils.invoke(
            overlay, "getPrayerImage", new Class<?>[] {HeadIcon.class}, HeadIcon.WRATH));
    assertNull(
        TestUtils.invoke(
            overlay, "getPrayerImage", new Class<?>[] {HeadIcon.class}, HeadIcon.RANGE_MAGE));
  }

  @Test
  public void getSkullImageCoversMappedAndDefaultCases() {
    BufferedImage image = image(8, 8);
    when(spriteManager.getSprite(anyInt(), eq(0))).thenReturn(image);

    assertSame(
        image,
        TestUtils.invoke(overlay, "getSkullImage", new Class<?>[] {int.class}, SkullIcon.SKULL));
    assertSame(
        image,
        TestUtils.invoke(
            overlay, "getSkullImage", new Class<?>[] {int.class}, SkullIcon.SKULL_FIGHT_PIT));
    assertNull(
        TestUtils.invoke(overlay, "getSkullImage", new Class<?>[] {int.class}, SkullIcon.NONE));
  }

  @Test
  public void renderHitsplatsHandlesEmptyAndLiveHitsplatsAndRestoresGraphicsState() {
    Graphics2D graphics = newGraphics();
    Actor actor = Mockito.mock(Actor.class);
    LocalPoint localPoint = new LocalPoint(128, 128, 0);
    Font originalFont = graphics.getFont();
    Color originalColor = graphics.getColor();

    when(plugin.getTrackedHitsplats(actor)).thenReturn(Collections.emptyList());
    TestUtils.invoke(
        overlay,
        "renderHitsplats",
        new Class<?>[] {Graphics2D.class, Actor.class, LocalPoint.class},
        graphics,
        actor,
        localPoint);
    assertEquals(originalFont, graphics.getFont());

    when(actor.getLogicalHeight()).thenReturn(40);
    when(client.getGameCycle()).thenReturn(100);
    when(plugin.getTrackedHitsplats(actor))
        .thenReturn(
            List.of(
                TestUtils.mirroredHitsplat(HitsplatID.BLOCK_OTHER, 15, 130),
                TestUtils.mirroredHitsplat(HitsplatID.BURN, 9, 105)));

    try (MockedStatic<net.runelite.api.Perspective> perspective =
            Mockito.mockStatic(net.runelite.api.Perspective.class);
        MockedStatic<OverlayUtil> overlayUtil = Mockito.mockStatic(OverlayUtil.class)) {
      perspective
          .when(
              () ->
                  net.runelite.api.Perspective.getCanvasTextLocation(
                      client, graphics, localPoint, "15", 112))
          .thenReturn(new Point(10, 10));
      perspective
          .when(
              () ->
                  net.runelite.api.Perspective.getCanvasTextLocation(
                      client, graphics, localPoint, "9", 138))
          .thenReturn(new Point(20, 20));

      TestUtils.invoke(
          overlay,
          "renderHitsplats",
          new Class<?>[] {Graphics2D.class, Actor.class, LocalPoint.class},
          graphics,
          actor,
          localPoint);

      overlayUtil.verify(
          () ->
              OverlayUtil.renderTextLocation(
                  graphics, new Point(10, 10), "15", new Color(96, 160, 255, 240)));
      overlayUtil.verify(
          () ->
              OverlayUtil.renderTextLocation(
                  graphics, new Point(20, 20), "9", new Color(255, 120, 64, 90)));
    }

    assertEquals(originalFont, graphics.getFont());
    assertEquals(originalColor, graphics.getColor());
  }

  @Test
  public void getHitsplatColorAndTextColorAndAlphaCoverRemainingCases() {
    Player player = Mockito.mock(Player.class);
    NPC npc = Mockito.mock(NPC.class);
    when(config.playerOutlineColor()).thenReturn(new Color(1, 2, 3, 4));
    when(config.npcOutlineColor()).thenReturn(new Color(5, 6, 7, 8));

    assertEquals(
        new Color(64, 224, 96),
        TestUtils.invoke(
            overlay, "getHitsplatColor", new Class<?>[] {int.class}, HitsplatID.POISON));
    assertEquals(
        new Color(120, 255, 120),
        TestUtils.invoke(overlay, "getHitsplatColor", new Class<?>[] {int.class}, HitsplatID.HEAL));
    assertEquals(
        new Color(170, 120, 255),
        TestUtils.invoke(
            overlay, "getHitsplatColor", new Class<?>[] {int.class}, HitsplatID.PRAYER_DRAIN));
    assertEquals(
        new Color(200, 80, 200),
        TestUtils.invoke(
            overlay, "getHitsplatColor", new Class<?>[] {int.class}, HitsplatID.CORRUPTION));
    assertEquals(
        new Color(255, 255, 255),
        TestUtils.invoke(overlay, "getHitsplatColor", new Class<?>[] {int.class}, -1));
    assertEquals(
        new Color(1, 2, 3, 4),
        TestUtils.invoke(overlay, "getTextColor", new Class<?>[] {Actor.class}, player));
    assertEquals(
        new Color(5, 6, 7, 8),
        TestUtils.invoke(overlay, "getTextColor", new Class<?>[] {Actor.class}, npc));
    assertEquals(
        new Color(9, 8, 7, 6),
        TestUtils.invoke(
            overlay, "withAlpha", new Class<?>[] {Color.class, int.class}, new Color(9, 8, 7), 6));
  }

  @Test
  public void getExtraVerticalOffsetAddsOnlyEnabledOverlayHeights() {
    Player player = Mockito.mock(Player.class);
    BufferedImage skullImage = image(12, 12);
    BufferedImage prayerImage = image(10, 10);

    when(config.mirrorHealthBars()).thenReturn(true);
    when(config.mirrorHeadIcons()).thenReturn(true);
    when(config.mirrorHitsplats()).thenReturn(true);
    when(player.getHealthScale()).thenReturn(20);
    when(player.getHealthRatio()).thenReturn(10);
    when(player.getSkullIcon()).thenReturn(SkullIcon.SKULL);
    when(player.getOverheadIcon()).thenReturn(HeadIcon.MELEE);
    when(plugin.getTrackedHitsplats(player))
        .thenReturn(List.of(TestUtils.mirroredHitsplat(HitsplatID.BLOCK_OTHER, 1, 110)));
    when(spriteManager.getSprite(anyInt(), eq(0))).thenReturn(skullImage, prayerImage);

    assertEquals(
        52,
        (int)
            TestUtils.invoke(
                overlay, "getExtraVerticalOffset", new Class<?>[] {Actor.class}, player));

    when(config.mirrorHealthBars()).thenReturn(false);
    when(config.mirrorHeadIcons()).thenReturn(false);
    when(config.mirrorHitsplats()).thenReturn(false);
    assertEquals(
        0,
        (int)
            TestUtils.invoke(
                overlay, "getExtraVerticalOffset", new Class<?>[] {Actor.class}, player));
  }

  private static BufferedImage getHealthBarAnchor() {
    return TestUtils.getField(
        TrueTileRendererOverlay.class, "HEALTH_BAR_ANCHOR", BufferedImage.class);
  }

  private static BufferedImage image(int width, int height) {
    return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
  }

  private static Graphics2D newGraphics() {
    return image(80, 80).createGraphics();
  }
}
