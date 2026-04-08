package com.truetilerenderer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import org.junit.Test;

public class TrueTileRendererConfigTest {
  private final TrueTileRendererConfig config = new DefaultConfig();

  @Test
  public void defaultValuesMatchPluginExpectations() {
    assertTrue(config.renderLocalPlayer());
    assertFalse(config.renderNpcs());
    assertEquals(NpcRenderMode.CURRENT_TARGET, config.npcRenderMode());
    assertEquals("", config.npcNameList());
    assertFalse(config.mirrorActorNames());
    assertTrue(config.mirrorHealthBars());
    assertTrue(config.mirrorHeadIcons());
    assertTrue(config.mirrorHitsplats());
    assertTrue(config.hideOriginalOverlays());
    assertEquals(new Color(0, 255, 255, 180), config.playerOutlineColor());
    assertEquals(new Color(255, 64, 64, 180), config.npcOutlineColor());
    assertEquals(4, config.outlineWidth());
    assertEquals(2, config.outlineFeather());
  }

  private static final class DefaultConfig implements TrueTileRendererConfig {}
}
