package com.truetilerenderer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.HeadIcon;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.SkullIcon;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

class TrueTileRendererOverlay extends Overlay
{
	private static final int NAME_OFFSET = 40;
	private static final Color TEXT_COLOR = Color.WHITE;
	private static final Color HEALTH_BAR_FILL = new Color(42, 170, 68, 220);
	private static final Color HEALTH_BAR_MISSING = new Color(120, 24, 24, 180);
	private static final Color HEALTH_BAR_BORDER = new Color(0, 0, 0, 220);
	private static final int HEALTH_BAR_WIDTH = 34;
	private static final int HEALTH_BAR_HEIGHT = 6;
	private static final int ICON_SPACING = 2;

	private final Client client;
	private final TrueTileRendererPlugin plugin;
	private final TrueTileRendererConfig config;
	private final SpriteManager spriteManager;

	@Inject
	private TrueTileRendererOverlay(Client client, TrueTileRendererPlugin plugin, TrueTileRendererConfig config, SpriteManager spriteManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.spriteManager = spriteManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		plugin.renderOutlines();

		if (config.renderLocalPlayer())
		{
			renderActorText(graphics, client.getLocalPlayer());
		}

		if (config.renderNpcs())
		{
			for (Actor actor : plugin.getTrackedNpcs())
			{
				renderActorText(graphics, actor);
			}
		}

		return null;
	}

	private void renderActorText(Graphics2D graphics, Actor actor)
	{
		if (actor == null)
		{
			return;
		}

		LocalPoint trueTileLocation = plugin.getTrueTileLocation(actor);
		if (trueTileLocation == null)
		{
			return;
		}

		if (config.mirrorActorNames())
		{
			String actorName = actor.getName();
			if (actorName != null && !actorName.isBlank())
			{
				Point nameLocation = Perspective.getCanvasTextLocation(client, graphics, trueTileLocation, actorName, actor.getLogicalHeight() + NAME_OFFSET + getExtraVerticalOffset(actor));
				OverlayUtil.renderTextLocation(graphics, nameLocation, actorName, getTextColor(actor));
			}
		}

		if (config.mirrorOverheadText())
		{
			String overheadText = actor.getOverheadText();
			if (overheadText != null && !overheadText.isBlank())
			{
				Point overheadLocation = Perspective.getCanvasTextLocation(client, graphics, trueTileLocation, overheadText, actor.getLogicalHeight() + NAME_OFFSET + getExtraVerticalOffset(actor) + 20);
				OverlayUtil.renderTextLocation(graphics, overheadLocation, overheadText, TEXT_COLOR);
			}
		}

		if (config.mirrorHealthBars())
		{
			renderHealthBar(graphics, actor, trueTileLocation);
		}

		if (config.mirrorHeadIcons())
		{
			renderHeadIcons(graphics, actor, trueTileLocation);
		}
	}

	private Color getTextColor(Actor actor)
	{
		if (actor instanceof Player)
		{
			return config.playerOutlineColor();
		}

		return config.npcOutlineColor();
	}

	private int getExtraVerticalOffset(Actor actor)
	{
		int offset = 0;
		if (config.mirrorHealthBars() && actor.getHealthScale() > 0 && actor.getHealthRatio() >= 0)
		{
			offset += 14;
		}

		if (config.mirrorHeadIcons() && !getHeadIconImages(actor).isEmpty())
		{
			offset += 20;
		}

		return offset;
	}

	private void renderHealthBar(Graphics2D graphics, Actor actor, LocalPoint trueTileLocation)
	{
		int healthScale = actor.getHealthScale();
		int healthRatio = actor.getHealthRatio();
		if (healthScale <= 0 || healthRatio < 0)
		{
			return;
		}

		int clampedRatio = Math.min(healthRatio, healthScale);
		int fillWidth = healthScale == 0 ? 0 : Math.round((clampedRatio / (float) healthScale) * HEALTH_BAR_WIDTH);
		BufferedImage anchor = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Point location = Perspective.getCanvasImageLocation(client, trueTileLocation, anchor, actor.getLogicalHeight() + 34);
		if (location == null)
		{
			return;
		}

		int x = location.getX() - (HEALTH_BAR_WIDTH / 2);
		int y = location.getY();

		graphics.setColor(HEALTH_BAR_BORDER);
		graphics.setStroke(new BasicStroke(1f));
		graphics.drawRect(x - 1, y - 1, HEALTH_BAR_WIDTH + 1, HEALTH_BAR_HEIGHT + 1);
		graphics.setColor(HEALTH_BAR_MISSING);
		graphics.fillRect(x, y, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);
		graphics.setColor(HEALTH_BAR_FILL);
		graphics.fillRect(x, y, fillWidth, HEALTH_BAR_HEIGHT);
	}

	private void renderHeadIcons(Graphics2D graphics, Actor actor, LocalPoint trueTileLocation)
	{
		List<BufferedImage> icons = getHeadIconImages(actor);
		if (icons.isEmpty())
		{
			return;
		}

		int totalWidth = icons.stream().mapToInt(BufferedImage::getWidth).sum() + ICON_SPACING * Math.max(0, icons.size() - 1);
		int startXOffset = -(totalWidth / 2);
		int runningXOffset = 0;
		int zOffset = actor.getLogicalHeight() + 52;

		for (BufferedImage icon : icons)
		{
			Point location = Perspective.getCanvasImageLocation(client, trueTileLocation, icon, zOffset);
			if (location != null)
			{
				Point shifted = new Point(location.getX() + startXOffset + runningXOffset, location.getY());
				OverlayUtil.renderImageLocation(graphics, shifted, icon);
			}
			runningXOffset += icon.getWidth() + ICON_SPACING;
		}
	}

	private List<BufferedImage> getHeadIconImages(Actor actor)
	{
		List<BufferedImage> images = new ArrayList<>();
		if (actor instanceof Player)
		{
			Player player = (Player) actor;
			addIfPresent(images, getSkullImage(player.getSkullIcon()));
			addIfPresent(images, getPrayerImage(player.getOverheadIcon()));
		}
		else if (actor instanceof NPC)
		{
			NPC npc = (NPC) actor;
			int[] archiveIds = npc.getOverheadArchiveIds();
			short[] spriteIds = npc.getOverheadSpriteIds();
			if (archiveIds != null && spriteIds != null)
			{
				for (int i = 0; i < Math.min(archiveIds.length, spriteIds.length); i++)
				{
					if (archiveIds[i] >= 0 && spriteIds[i] >= 0)
					{
						addIfPresent(images, spriteManager.getSprite(archiveIds[i], spriteIds[i]));
					}
				}
			}
		}
		return images;
	}

	private void addIfPresent(List<BufferedImage> images, BufferedImage image)
	{
		if (image != null)
		{
			images.add(image);
		}
	}

	private BufferedImage getPrayerImage(HeadIcon headIcon)
	{
		if (headIcon == null)
		{
			return null;
		}

		switch (headIcon)
		{
			case MELEE:
				return spriteManager.getSprite(SpriteID.Prayeron.PROTECT_FROM_MELEE, 0);
			case RANGED:
				return spriteManager.getSprite(SpriteID.Prayeron.PROTECT_FROM_MISSILES, 0);
			case MAGIC:
				return spriteManager.getSprite(SpriteID.Prayeron.PROTECT_FROM_MAGIC, 0);
			case RETRIBUTION:
				return spriteManager.getSprite(SpriteID.Prayeron.RETRIBUTION, 0);
			case SMITE:
				return spriteManager.getSprite(SpriteID.Prayeron.SMITE, 0);
			case REDEMPTION:
				return spriteManager.getSprite(SpriteID.Prayeron.REDEMPTION, 0);
			case WRATH:
				return spriteManager.getSprite(SpriteID.IconPrayerZaros01_30x30.WRATH, 0);
			default:
				return null;
		}
	}

	private BufferedImage getSkullImage(int skullIcon)
	{
		switch (skullIcon)
		{
			case SkullIcon.SKULL:
				return spriteManager.getSprite(SpriteID.HeadiconsPkInterface.PLAYER_KILLER_SKULL, 0);
			case SkullIcon.SKULL_FIGHT_PIT:
				return spriteManager.getSprite(SpriteID.HeadiconsPkInterface.FIGHT_PITS_WINNER_SKULL_RED, 0);
			default:
				return null;
		}
	}
}
