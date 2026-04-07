package com.truetilerenderer;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(TrueTileRendererConfig.GROUP)
public interface TrueTileRendererConfig extends Config {
  String GROUP = "trueTileRenderer";

  @ConfigItem(
      keyName = "renderLocalPlayer",
      name = "Hide local model, show true-tile outline",
      description = "Hide your live local player model and render its outline on the server tile",
      position = 0)
  default boolean renderLocalPlayer() {
    return true;
  }

  @ConfigItem(
      keyName = "renderNpcs",
      name = "Hide NPC models, show true-tile outlines",
      description = "Hide matching NPC models and render their outlines on their server tiles",
      position = 1)
  default boolean renderNpcs() {
    return false;
  }

  @ConfigItem(
      keyName = "npcRenderMode",
      name = "NPC target mode",
      description =
          "Choose whether to outline only your current combat target or a configured NPC list",
      position = 2)
  default NpcRenderMode npcRenderMode() {
    return NpcRenderMode.CURRENT_TARGET;
  }

  @ConfigItem(
      keyName = "npcNameList",
      name = "NPC list",
      description =
          "Comma or newline separated NPC names to render when using Configured list mode",
      position = 3)
  default String npcNameList() {
    return "";
  }

  @ConfigItem(
      keyName = "mirrorActorNames",
      name = "Mirror actor names",
      description = "Draw actor names at the true tile position",
      position = 4)
  default boolean mirrorActorNames() {
    return false;
  }

  @ConfigItem(
      keyName = "mirrorHealthBars",
      name = "Mirror health bars",
      description = "Draw custom health bars at the true tile position",
      position = 5)
  default boolean mirrorHealthBars() {
    return true;
  }

  @ConfigItem(
      keyName = "mirrorHeadIcons",
      name = "Mirror overheads",
      description = "Draw overhead prayer and skull icons at the true tile position",
      position = 6)
  default boolean mirrorHeadIcons() {
    return true;
  }

  @ConfigItem(
      keyName = "mirrorHitsplats",
      name = "Mirror hitsplats",
      description = "Draw custom hitsplats at the true tile position",
      position = 7)
  default boolean mirrorHitsplats() {
    return true;
  }

  @Alpha
  @ConfigItem(
      keyName = "playerOutlineColor",
      name = "Player outline color",
      description = "Outline color used for your local player",
      position = 8)
  default Color playerOutlineColor() {
    return new Color(0, 255, 255, 180);
  }

  @Alpha
  @ConfigItem(
      keyName = "npcOutlineColor",
      name = "NPC outline color",
      description = "Outline color used for matching NPCs",
      position = 9)
  default Color npcOutlineColor() {
    return new Color(255, 64, 64, 180);
  }

  @ConfigItem(
      keyName = "outlineWidth",
      name = "Outline width",
      description = "Width of the rendered outline",
      position = 10)
  default int outlineWidth() {
    return 4;
  }

  @ConfigItem(
      keyName = "outlineFeather",
      name = "Outline feather",
      description = "Softness of the rendered outline",
      position = 11)
  default int outlineFeather() {
    return 2;
  }
}
