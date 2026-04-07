package com.truetilerenderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

class TrueTileRendererOverlay extends Overlay
{
	private static final int NAME_OFFSET = 40;
	private static final Color TEXT_COLOR = Color.WHITE;

	private final Client client;
	private final TrueTileRendererPlugin plugin;
	private final TrueTileRendererConfig config;

	@Inject
	private TrueTileRendererOverlay(Client client, TrueTileRendererPlugin plugin, TrueTileRendererConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
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
				Point nameLocation = Perspective.getCanvasTextLocation(client, graphics, trueTileLocation, actorName, actor.getLogicalHeight() + NAME_OFFSET);
				OverlayUtil.renderTextLocation(graphics, nameLocation, actorName, getTextColor(actor));
			}
		}

		if (config.mirrorOverheadText())
		{
			String overheadText = actor.getOverheadText();
			if (overheadText != null && !overheadText.isBlank())
			{
				Point overheadLocation = Perspective.getCanvasTextLocation(client, graphics, trueTileLocation, overheadText, actor.getLogicalHeight() + NAME_OFFSET + 20);
				OverlayUtil.renderTextLocation(graphics, overheadLocation, overheadText, TEXT_COLOR);
			}
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
}
