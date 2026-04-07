package com.truetilerenderer;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Renderable;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.HitsplatApplied;
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
    tags = {"combat", "npc", "outline", "player", "true tile"})
public class TrueTileRendererPlugin extends Plugin {
  @Inject private Client client;

  @Inject private TrueTileRendererConfig config;

  @Inject private Hooks hooks;

  @Inject private OverlayManager overlayManager;

  @Inject private TrueTileRendererOverlay overlay;

  @Inject private ModelOutlineRenderer modelOutlineRenderer;

  @SuppressWarnings("deprecation")
  private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;

  private final IdentityHashMap<Actor, RuneLiteObject> outlineObjects = new IdentityHashMap<>();
  private final IdentityHashMap<Actor, List<MirroredHitsplat>> mirroredHitsplats =
      new IdentityHashMap<>();
  private Set<String> configuredNpcNames = Collections.emptySet();

  @Override
  @SuppressWarnings("deprecation")
  protected void startUp() {
    updateConfig();
    overlayManager.add(overlay);
    hooks.registerRenderableDrawListener(drawListener);
  }

  @Override
  @SuppressWarnings("deprecation")
  protected void shutDown() {
    hooks.unregisterRenderableDrawListener(drawListener);
    overlayManager.remove(overlay);
    outlineObjects.clear();
    mirroredHitsplats.clear();
  }

  @Subscribe
  public void onConfigChanged(ConfigChanged event) {
    if (TrueTileRendererConfig.GROUP.equals(event.getGroup())) {
      updateConfig();
    }
  }

  @Subscribe
  public void onHitsplatApplied(HitsplatApplied event) {
    if (!config.mirrorHitsplats()) {
      return;
    }

    Actor actor = event.getActor();
    Hitsplat hitsplat = event.getHitsplat();
    if (actor == null || hitsplat == null) {
      return;
    }

    if (!isTrackedActor(actor)) {
      mirroredHitsplats.remove(actor);
      return;
    }

    mirroredHitsplats
        .computeIfAbsent(actor, ignored -> new ArrayList<>())
        .add(
            new MirroredHitsplat(
                hitsplat.getHitsplatType(),
                hitsplat.getAmount(),
                hitsplat.getDisappearsOnGameCycle()));
  }

  @Subscribe
  public void onActorDeath(ActorDeath event) {
    if (event.getActor() != null) {
      mirroredHitsplats.remove(event.getActor());
    }
  }

  void renderOutlines() {
    if (client.getGameState() != GameState.LOGGED_IN) {
      outlineObjects.clear();
      mirroredHitsplats.clear();
      return;
    }

    Set<Actor> trackedActors = new HashSet<>();
    renderLocalPlayerOutline(trackedActors);
    for (NPC npc : getTrackedNpcs()) {
      trackedActors.add(npc);
      drawActorOutline(npc, config.npcOutlineColor());
    }

    pruneTransientState(trackedActors);
  }

  Set<NPC> getTrackedNpcs() {
    if (!config.renderNpcs()
        || client.getLocalPlayer() == null
        || client.getTopLevelWorldView() == null) {
      return Collections.emptySet();
    }

    Set<NPC> npcs = new LinkedHashSet<>();
    for (NPC npc : client.getTopLevelWorldView().npcs()) {
      if (TrueTileActorMatcher.shouldRenderNpc(
          npc, client.getLocalPlayer(), config.npcRenderMode(), configuredNpcNames)) {
        npcs.add(npc);
      }
    }
    return npcs;
  }

  List<MirroredHitsplat> getTrackedHitsplats(Actor actor) {
    if (actor == null) {
      return Collections.emptyList();
    }

    List<MirroredHitsplat> hitsplats = mirroredHitsplats.get(actor);
    if (hitsplats == null || hitsplats.isEmpty()) {
      return Collections.emptyList();
    }

    int currentCycle = client.getGameCycle();
    hitsplats.removeIf(hitsplat -> hitsplat.getDisappearsOnGameCycle() <= currentCycle);
    if (hitsplats.isEmpty()) {
      mirroredHitsplats.remove(actor);
      return Collections.emptyList();
    }

    return Collections.unmodifiableList(new ArrayList<>(hitsplats));
  }

  LocalPoint getTrueTileLocation(Actor actor) {
    WorldPoint worldLocation = actor.getWorldLocation();
    if (worldLocation == null) {
      return null;
    }

    return LocalPoint.fromWorld(actor.getWorldView(), worldLocation);
  }

  private void renderLocalPlayerOutline(Set<Actor> trackedActors) {
    if (!config.renderLocalPlayer()) {
      return;
    }

    Player localPlayer = client.getLocalPlayer();
    if (localPlayer != null) {
      trackedActors.add(localPlayer);
      drawActorOutline(localPlayer, config.playerOutlineColor());
    }
  }

  private void drawActorOutline(Actor actor, Color color) {
    LocalPoint trueTileLocation = getTrueTileLocation(actor);
    if (trueTileLocation == null) {
      return;
    }

    var model = actor.getModel();
    if (model == null) {
      return;
    }

    RuneLiteObject outlineObject =
        outlineObjects.computeIfAbsent(actor, ignored -> client.createRuneLiteObject());
    outlineObject.setModel(model);
    outlineObject.setLocation(trueTileLocation, actor.getWorldView().getPlane());
    outlineObject.setOrientation(actor.getCurrentOrientation());
    modelOutlineRenderer.drawOutline(
        outlineObject, config.outlineWidth(), color, config.outlineFeather());
  }

  private boolean shouldDraw(Renderable renderable, boolean drawingUi) {
    if (drawingUi) {
      return !(renderable instanceof Actor) || !shouldSuppressOriginalUi((Actor) renderable);
    }

    Player localPlayer = client.getLocalPlayer();
    if (config.renderLocalPlayer() && renderable == localPlayer) {
      return false;
    }

    if (config.renderNpcs()
        && renderable instanceof NPC
        && TrueTileActorMatcher.shouldRenderNpc(
            (NPC) renderable, localPlayer, config.npcRenderMode(), configuredNpcNames)) {
      return false;
    }

    return true;
  }

  private boolean shouldSuppressOriginalUi(Actor actor) {
    if (!config.hideOriginalOverlays() || !isTrackedActor(actor)) {
      return false;
    }

    // Prevent duplicate overhead UI when the plugin is already redrawing those elements
    // at the actor's true-tile position.
    return config.mirrorActorNames()
        || config.mirrorHealthBars()
        || config.mirrorHeadIcons()
        || config.mirrorHitsplats();
  }

  private void pruneTransientState(Set<Actor> trackedActors) {
    pruneOutlineObjects(trackedActors);
    pruneMirroredHitsplats(trackedActors);
  }

  private void pruneOutlineObjects(Set<Actor> trackedActors) {
    Iterator<Actor> iterator = outlineObjects.keySet().iterator();
    while (iterator.hasNext()) {
      if (!trackedActors.contains(iterator.next())) {
        iterator.remove();
      }
    }
  }

  private void pruneMirroredHitsplats(Set<Actor> trackedActors) {
    int currentCycle = client.getGameCycle();
    Iterator<Map.Entry<Actor, List<MirroredHitsplat>>> iterator =
        mirroredHitsplats.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<Actor, List<MirroredHitsplat>> entry = iterator.next();
      List<MirroredHitsplat> hitsplats = entry.getValue();
      hitsplats.removeIf(hitsplat -> hitsplat.getDisappearsOnGameCycle() <= currentCycle);
      if (!trackedActors.contains(entry.getKey()) || hitsplats.isEmpty()) {
        iterator.remove();
      }
    }
  }

  private boolean isTrackedActor(Actor actor) {
    if (actor == null) {
      return false;
    }

    if (actor == client.getLocalPlayer()) {
      return config.renderLocalPlayer();
    }

    if (actor instanceof NPC) {
      return config.renderNpcs()
          && TrueTileActorMatcher.shouldRenderNpc(
              (NPC) actor, client.getLocalPlayer(), config.npcRenderMode(), configuredNpcNames);
    }

    return false;
  }

  private void updateConfig() {
    configuredNpcNames = TrueTileActorMatcher.parseConfiguredNpcNames(config.npcNameList());
    if (!config.mirrorHitsplats()) {
      mirroredHitsplats.clear();
    }
  }

  @Provides
  TrueTileRendererConfig provideConfig(ConfigManager configManager) {
    return configManager.getConfig(TrueTileRendererConfig.class);
  }

  static final class MirroredHitsplat {
    private final int hitsplatType;
    private final int amount;
    private final int disappearsOnGameCycle;

    private MirroredHitsplat(int hitsplatType, int amount, int disappearsOnGameCycle) {
      this.hitsplatType = hitsplatType;
      this.amount = amount;
      this.disappearsOnGameCycle = disappearsOnGameCycle;
    }

    int getHitsplatType() {
      return hitsplatType;
    }

    int getAmount() {
      return amount;
    }

    int getDisappearsOnGameCycle() {
      return disappearsOnGameCycle;
    }
  }
}
