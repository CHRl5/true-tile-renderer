package com.truetilerenderer;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Renderable;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

@PluginDescriptor(
	name = "True Tile Renderer",
	description = "Hide specific actors and render their outlines on their server tiles",
	tags = {"combat", "npc", "outline", "player", "true tile"}
)
public class TrueTileRendererPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private TrueTileRendererConfig config;

	@Inject
	private Hooks hooks;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private TrueTileRendererOverlay overlay;

	@Inject
	private ModelOutlineRenderer modelOutlineRenderer;

	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;
	private final IdentityHashMap<Actor, RuneLiteObject> outlineObjects = new IdentityHashMap<>();
	private Set<String> configuredNpcNames = Collections.emptySet();

	@Override
	protected void startUp()
	{
		updateConfig();
		overlayManager.add(overlay);
		hooks.registerRenderableDrawListener(drawListener);
	}

	@Override
	protected void shutDown()
	{
		hooks.unregisterRenderableDrawListener(drawListener);
		overlayManager.remove(overlay);
		outlineObjects.clear();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (TrueTileRendererConfig.GROUP.equals(event.getGroup()))
		{
			updateConfig();
		}
	}

	void renderOutlines()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		renderLocalPlayerOutline();
		for (NPC npc : getTrackedNpcs())
		{
			drawActorOutline(npc, config.npcOutlineColor());
		}
	}

	Set<NPC> getTrackedNpcs()
	{
		if (!config.renderNpcs() || client.getLocalPlayer() == null || client.getTopLevelWorldView() == null)
		{
			return Collections.emptySet();
		}

		Set<NPC> npcs = new LinkedHashSet<>();
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (TrueTileActorMatcher.shouldRenderNpc(npc, client.getLocalPlayer(), config.npcRenderMode(), configuredNpcNames))
			{
				npcs.add(npc);
			}
		}
		return npcs;
	}

	LocalPoint getTrueTileLocation(Actor actor)
	{
		WorldPoint worldLocation = actor.getWorldLocation();
		if (worldLocation == null)
		{
			return null;
		}

		return LocalPoint.fromWorld(actor.getWorldView(), worldLocation);
	}

	private void renderLocalPlayerOutline()
	{
		if (!config.renderLocalPlayer())
		{
			return;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer != null)
		{
			drawActorOutline(localPlayer, config.playerOutlineColor());
		}
	}

	private void drawActorOutline(Actor actor, Color color)
	{
		LocalPoint trueTileLocation = getTrueTileLocation(actor);
		if (trueTileLocation == null)
		{
			return;
		}

		var model = actor.getModel();
		if (model == null)
		{
			return;
		}

		RuneLiteObject outlineObject = outlineObjects.computeIfAbsent(actor, ignored -> client.createRuneLiteObject());
		outlineObject.setModel(model);
		outlineObject.setLocation(trueTileLocation, actor.getWorldView().getPlane());
		outlineObject.setOrientation(actor.getCurrentOrientation());
		modelOutlineRenderer.drawOutline(outlineObject, config.outlineWidth(), color, config.outlineFeather());
	}

	private boolean shouldDraw(Renderable renderable, boolean drawingUi)
	{
		if (drawingUi)
		{
			return true;
		}

		Player localPlayer = client.getLocalPlayer();
		if (config.renderLocalPlayer() && renderable == localPlayer)
		{
			return false;
		}

		if (config.renderNpcs() && renderable instanceof NPC
			&& TrueTileActorMatcher.shouldRenderNpc((NPC) renderable, localPlayer, config.npcRenderMode(), configuredNpcNames))
		{
			return false;
		}

		return true;
	}

	private void updateConfig()
	{
		configuredNpcNames = TrueTileActorMatcher.parseConfiguredNpcNames(config.npcNameList());
	}

	@Provides
	TrueTileRendererConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TrueTileRendererConfig.class);
	}
}
