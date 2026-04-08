package com.truetilerenderer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Renderable;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class TrueTileRendererPluginBehaviorTest {
  private final Color playerColor = new Color(0, 255, 255, 180);
  private final Color npcColor = new Color(255, 64, 64, 180);

  private Client client;
  private TrueTileRendererConfig config;
  private Hooks hooks;
  private OverlayManager overlayManager;
  private TrueTileRendererOverlay overlay;
  private ModelOutlineRenderer modelOutlineRenderer;
  private WorldView worldView;
  private Player localPlayer;
  private TrueTileRendererPlugin plugin;

  @Before
  public void setUp() {
    client = Mockito.mock(Client.class);
    config = Mockito.mock(TrueTileRendererConfig.class);
    hooks = Mockito.mock(Hooks.class);
    overlayManager = Mockito.mock(OverlayManager.class);
    overlay = Mockito.mock(TrueTileRendererOverlay.class);
    modelOutlineRenderer = Mockito.mock(ModelOutlineRenderer.class);
    worldView = Mockito.mock(WorldView.class);
    localPlayer = TestUtils.mockPlayer(worldView);

    plugin = new TrueTileRendererPlugin();
    TestUtils.setField(plugin, "client", client);
    TestUtils.setField(plugin, "config", config);
    TestUtils.setField(plugin, "hooks", hooks);
    TestUtils.setField(plugin, "overlayManager", overlayManager);
    TestUtils.setField(plugin, "overlay", overlay);
    TestUtils.setField(plugin, "modelOutlineRenderer", modelOutlineRenderer);

    when(client.getLocalPlayer()).thenReturn(localPlayer);
    when(client.getTopLevelWorldView()).thenReturn(worldView);
    when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
    when(client.getGameCycle()).thenReturn(100);
    when(worldView.getPlane()).thenReturn(0);

    when(config.npcNameList()).thenReturn("");
    when(config.npcRenderMode()).thenReturn(NpcRenderMode.CURRENT_TARGET);
    when(config.playerOutlineColor()).thenReturn(playerColor);
    when(config.npcOutlineColor()).thenReturn(npcColor);
    when(config.outlineWidth()).thenReturn(4);
    when(config.outlineFeather()).thenReturn(2);
  }

  @Test
  public void startUpRegistersOverlayAndHookAndLoadsConfiguredNpcNames() {
    when(config.npcNameList()).thenReturn("Zebak, Kephri");
    when(config.mirrorHitsplats()).thenReturn(true);
    when(config.renderNpcs()).thenReturn(true);

    plugin.startUp();

    verify(overlayManager).add(overlay);
    verifyHooksRegistered();
    assertEquals(Set.of("zebak", "kephri"), getConfiguredNpcNames());
  }

  @Test
  public void shutDownUnregistersOverlayAndClearsTransientState() {
    getOutlineObjects().put(localPlayer, Mockito.mock(RuneLiteObject.class));
    getMirroredHitsplats()
        .put(localPlayer, new ArrayList<>(List.of(TestUtils.mirroredHitsplat(1, 12, 150))));
    TestUtils.setField(plugin, "trackedNpcs", new LinkedHashSet<>(Set.of(Mockito.mock(NPC.class))));
    TestUtils.setField(plugin, "drawListenerErrorLogged", true);

    plugin.shutDown();

    verifyHooksUnregistered();
    verify(overlayManager).remove(overlay);
    assertTrue(getOutlineObjects().isEmpty());
    assertTrue(getMirroredHitsplats().isEmpty());
    assertEquals(Collections.emptySet(), plugin.getTrackedNpcs());
    assertFalse(TestUtils.getField(plugin, "drawListenerErrorLogged", Boolean.class));
  }

  @Test
  public void onConfigChangedUpdatesOnlyPluginGroupAndClearsDisabledState() {
    TestUtils.setField(plugin, "configuredNpcNames", Set.of("old"));
    getMirroredHitsplats()
        .put(localPlayer, new ArrayList<>(List.of(TestUtils.mirroredHitsplat(1, 12, 150))));
    TestUtils.setField(plugin, "trackedNpcs", new LinkedHashSet<>(Set.of(Mockito.mock(NPC.class))));

    when(config.npcNameList()).thenReturn("Whisperer");
    when(config.mirrorHitsplats()).thenReturn(false);
    when(config.renderNpcs()).thenReturn(false);

    ConfigChanged unrelated = Mockito.mock(ConfigChanged.class);
    when(unrelated.getGroup()).thenReturn("other");
    plugin.onConfigChanged(unrelated);
    assertEquals(Set.of("old"), getConfiguredNpcNames());

    ConfigChanged relevant = Mockito.mock(ConfigChanged.class);
    when(relevant.getGroup()).thenReturn(TrueTileRendererConfig.GROUP);
    plugin.onConfigChanged(relevant);

    assertEquals(Set.of("whisperer"), getConfiguredNpcNames());
    assertTrue(getMirroredHitsplats().isEmpty());
    assertEquals(Collections.emptySet(), plugin.getTrackedNpcs());
  }

  @Test
  public void onHitsplatAppliedIgnoresDisabledOrIncompleteEvents() {
    HitsplatApplied event = Mockito.mock(HitsplatApplied.class);
    Hitsplat hitsplat = mockHitsplat(1, 8, 140);
    when(event.getActor()).thenReturn(localPlayer);
    when(event.getHitsplat()).thenReturn(hitsplat);

    when(config.mirrorHitsplats()).thenReturn(false);
    plugin.onHitsplatApplied(event);
    assertTrue(getMirroredHitsplats().isEmpty());

    when(config.mirrorHitsplats()).thenReturn(true);
    when(event.getActor()).thenReturn(null);
    plugin.onHitsplatApplied(event);
    assertTrue(getMirroredHitsplats().isEmpty());

    when(event.getActor()).thenReturn(localPlayer);
    when(event.getHitsplat()).thenReturn(null);
    plugin.onHitsplatApplied(event);
    assertTrue(getMirroredHitsplats().isEmpty());
  }

  @Test
  public void onHitsplatAppliedTracksLiveHitsplatsAndPrunesExpiredOnRead() {
    HitsplatApplied event = Mockito.mock(HitsplatApplied.class);
    Hitsplat hitsplat = mockHitsplat(3, 18, 130);

    when(config.mirrorHitsplats()).thenReturn(true);
    when(config.renderLocalPlayer()).thenReturn(true);
    when(event.getActor()).thenReturn(localPlayer);
    when(event.getHitsplat()).thenReturn(hitsplat);

    plugin.onHitsplatApplied(event);

    List<TrueTileRendererPlugin.MirroredHitsplat> trackedHitsplats =
        plugin.getTrackedHitsplats(localPlayer);
    assertEquals(1, trackedHitsplats.size());
    assertEquals(3, trackedHitsplats.get(0).getHitsplatType());
    assertEquals(18, trackedHitsplats.get(0).getAmount());
    assertEquals(130, trackedHitsplats.get(0).getDisappearsOnGameCycle());
    assertThrows(
        UnsupportedOperationException.class,
        () -> trackedHitsplats.add(TestUtils.mirroredHitsplat(1, 1, 1)));

    when(client.getGameCycle()).thenReturn(130);
    assertTrue(plugin.getTrackedHitsplats(localPlayer).isEmpty());
    assertTrue(plugin.getTrackedHitsplats(Mockito.mock(Actor.class)).isEmpty());
    assertTrue(plugin.getTrackedHitsplats(null).isEmpty());
  }

  @Test
  public void onHitsplatAppliedRemovesUntrackedActorsAndActorDeathRemovesEntries() {
    getMirroredHitsplats()
        .put(localPlayer, new ArrayList<>(List.of(TestUtils.mirroredHitsplat(1, 9, 150))));

    HitsplatApplied event = Mockito.mock(HitsplatApplied.class);
    Hitsplat hitsplat = mockHitsplat(1, 9, 150);
    when(config.mirrorHitsplats()).thenReturn(true);
    when(config.renderLocalPlayer()).thenReturn(false);
    when(event.getActor()).thenReturn(localPlayer);
    when(event.getHitsplat()).thenReturn(hitsplat);

    plugin.onHitsplatApplied(event);
    assertTrue(getMirroredHitsplats().isEmpty());

    getMirroredHitsplats()
        .put(localPlayer, new ArrayList<>(List.of(TestUtils.mirroredHitsplat(1, 9, 150))));
    ActorDeath death = Mockito.mock(ActorDeath.class);
    when(death.getActor()).thenReturn(localPlayer);
    plugin.onActorDeath(death);
    assertTrue(getMirroredHitsplats().isEmpty());

    when(death.getActor()).thenReturn(null);
    plugin.onActorDeath(death);
    assertTrue(getMirroredHitsplats().isEmpty());
  }

  @Test
  public void onGameStateChangedClearsOnlyWhenLeavingLoggedIn() {
    getOutlineObjects().put(localPlayer, Mockito.mock(RuneLiteObject.class));
    getMirroredHitsplats()
        .put(localPlayer, new ArrayList<>(List.of(TestUtils.mirroredHitsplat(1, 9, 150))));
    TestUtils.setField(plugin, "trackedNpcs", new LinkedHashSet<>(Set.of(Mockito.mock(NPC.class))));
    TestUtils.setField(plugin, "drawListenerErrorLogged", true);

    GameStateChanged loggedIn = Mockito.mock(GameStateChanged.class);
    when(loggedIn.getGameState()).thenReturn(GameState.LOGGED_IN);
    plugin.onGameStateChanged(loggedIn);
    assertFalse(getOutlineObjects().isEmpty());

    GameStateChanged loggedOut = Mockito.mock(GameStateChanged.class);
    when(loggedOut.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
    plugin.onGameStateChanged(loggedOut);
    assertTrue(getOutlineObjects().isEmpty());
    assertTrue(getMirroredHitsplats().isEmpty());
    assertEquals(Collections.emptySet(), plugin.getTrackedNpcs());
    assertFalse(TestUtils.getField(plugin, "drawListenerErrorLogged", Boolean.class));
  }

  @Test
  public void renderOutlinesClearsStateWhenClientIsNotLoggedIn() {
    getOutlineObjects().put(localPlayer, Mockito.mock(RuneLiteObject.class));
    getMirroredHitsplats()
        .put(localPlayer, new ArrayList<>(List.of(TestUtils.mirroredHitsplat(1, 9, 150))));
    when(client.getGameState()).thenReturn(GameState.CONNECTION_LOST);

    plugin.renderOutlines();

    assertTrue(getOutlineObjects().isEmpty());
    assertTrue(getMirroredHitsplats().isEmpty());
    assertEquals(Collections.emptySet(), plugin.getTrackedNpcs());
  }

  @Test
  public void renderOutlinesRendersTrackedPlayerAndNpcAndPrunesStaleEntries() {
    NPC trackedNpc = mockNpc("Zebak", worldView);
    NPC untrackedNpc = mockNpc("Vardorvis", worldView);
    Actor staleActor = Mockito.mock(Actor.class);

    Model localModel = Mockito.mock(Model.class);
    Model npcModel = Mockito.mock(Model.class);
    WorldPoint localWorldPoint = new WorldPoint(3200, 3200, 0);
    WorldPoint npcWorldPoint = new WorldPoint(3201, 3201, 0);
    LocalPoint localPoint = new LocalPoint(128, 128, worldView);
    LocalPoint npcPoint = new LocalPoint(256, 256, worldView);
    RuneLiteObject localOutline = Mockito.mock(RuneLiteObject.class);
    RuneLiteObject npcOutline = Mockito.mock(RuneLiteObject.class);

    when(config.renderLocalPlayer()).thenReturn(true);
    when(config.renderNpcs()).thenReturn(true);
    when(config.mirrorHitsplats()).thenReturn(true);
    when(config.npcRenderMode()).thenReturn(NpcRenderMode.CONFIGURED_LIST);
    when(config.npcNameList()).thenReturn("Zebak");
    when(localPlayer.getWorldLocation()).thenReturn(localWorldPoint);
    when(localPlayer.getModel()).thenReturn(localModel);
    when(localPlayer.getCurrentOrientation()).thenReturn(512);
    when(trackedNpc.getWorldLocation()).thenReturn(npcWorldPoint);
    when(trackedNpc.getModel()).thenReturn(npcModel);
    when(trackedNpc.getCurrentOrientation()).thenReturn(1024);
    when(client.createRuneLiteObject()).thenReturn(localOutline, npcOutline);
    Mockito.doReturn(TestUtils.indexedSetOf(trackedNpc, untrackedNpc)).when(worldView).npcs();

    getOutlineObjects().put(staleActor, Mockito.mock(RuneLiteObject.class));
    getMirroredHitsplats()
        .put(trackedNpc, new ArrayList<>(List.of(TestUtils.mirroredHitsplat(1, 9, 150))));
    getMirroredHitsplats()
        .put(staleActor, new ArrayList<>(List.of(TestUtils.mirroredHitsplat(1, 9, 150))));

    try (MockedStatic<LocalPoint> mockedLocalPoint = Mockito.mockStatic(LocalPoint.class)) {
      mockedLocalPoint
          .when(() -> LocalPoint.fromWorld(worldView, localWorldPoint))
          .thenReturn(localPoint);
      mockedLocalPoint
          .when(() -> LocalPoint.fromWorld(worldView, npcWorldPoint))
          .thenReturn(npcPoint);

      plugin.startUp();
      plugin.renderOutlines();
    }

    verify(localOutline).setModel(localModel);
    verify(localOutline).setLocation(localPoint, 0);
    verify(localOutline).setOrientation(512);
    verify(npcOutline).setModel(npcModel);
    verify(npcOutline).setLocation(npcPoint, 0);
    verify(npcOutline).setOrientation(1024);
    verify(modelOutlineRenderer).drawOutline(localOutline, 4, playerColor, 2);
    verify(modelOutlineRenderer).drawOutline(npcOutline, 4, npcColor, 2);
    assertEquals(Set.of(trackedNpc), plugin.getTrackedNpcs());
    assertEquals(Set.of(localPlayer, trackedNpc), getOutlineObjects().keySet());
    assertEquals(Set.of(trackedNpc), getMirroredHitsplats().keySet());
  }

  @Test
  public void collectTrackedNpcsReturnsEmptyWithoutRequiredState() {
    when(config.renderNpcs()).thenReturn(false);
    assertEquals(
        Collections.emptySet(), TestUtils.invoke(plugin, "collectTrackedNpcs", new Class<?>[] {}));

    when(config.renderNpcs()).thenReturn(true);
    when(client.getLocalPlayer()).thenReturn(null);
    assertEquals(
        Collections.emptySet(), TestUtils.invoke(plugin, "collectTrackedNpcs", new Class<?>[] {}));

    when(client.getLocalPlayer()).thenReturn(localPlayer);
    when(client.getTopLevelWorldView()).thenReturn(null);
    assertEquals(
        Collections.emptySet(), TestUtils.invoke(plugin, "collectTrackedNpcs", new Class<?>[] {}));
  }

  @Test
  public void getTrueTileLocationAndDrawActorOutlineHandleMissingData() {
    Actor actor = Mockito.mock(Actor.class);
    Model model = Mockito.mock(Model.class);
    WorldPoint worldPoint = new WorldPoint(3202, 3202, 0);
    LocalPoint trueTile = new LocalPoint(384, 384, worldView);
    RuneLiteObject outlineObject = Mockito.mock(RuneLiteObject.class);

    when(actor.getWorldView()).thenReturn(worldView);
    when(actor.getCurrentOrientation()).thenReturn(256);

    when(actor.getWorldLocation()).thenReturn(null);
    assertNull(plugin.getTrueTileLocation(actor));
    TestUtils.invoke(
        plugin, "drawActorOutline", new Class<?>[] {Actor.class, Color.class}, actor, playerColor);
    verifyNoInteractions(modelOutlineRenderer);

    when(actor.getWorldLocation()).thenReturn(worldPoint);
    when(actor.getModel()).thenReturn(null);
    try (MockedStatic<LocalPoint> mockedLocalPoint = Mockito.mockStatic(LocalPoint.class)) {
      mockedLocalPoint.when(() -> LocalPoint.fromWorld(worldView, worldPoint)).thenReturn(trueTile);
      assertSame(trueTile, plugin.getTrueTileLocation(actor));
      TestUtils.invoke(
          plugin,
          "drawActorOutline",
          new Class<?>[] {Actor.class, Color.class},
          actor,
          playerColor);
    }
    verifyNoInteractions(modelOutlineRenderer);

    when(actor.getModel()).thenReturn(model);
    when(client.createRuneLiteObject()).thenReturn(outlineObject);
    try (MockedStatic<LocalPoint> mockedLocalPoint = Mockito.mockStatic(LocalPoint.class)) {
      mockedLocalPoint.when(() -> LocalPoint.fromWorld(worldView, worldPoint)).thenReturn(trueTile);
      TestUtils.invoke(
          plugin,
          "drawActorOutline",
          new Class<?>[] {Actor.class, Color.class},
          actor,
          playerColor);
    }
    verify(modelOutlineRenderer).drawOutline(outlineObject, 4, playerColor, 2);
  }

  @Test
  public void renderLocalPlayerOutlineHonorsConfigAndMissingPlayer() {
    Set<Actor> trackedActors = new HashSet<>();

    when(config.renderLocalPlayer()).thenReturn(false);
    TestUtils.invoke(plugin, "renderLocalPlayerOutline", new Class<?>[] {Set.class}, trackedActors);
    assertTrue(trackedActors.isEmpty());

    when(config.renderLocalPlayer()).thenReturn(true);
    when(client.getLocalPlayer()).thenReturn(null);
    TestUtils.invoke(plugin, "renderLocalPlayerOutline", new Class<?>[] {Set.class}, trackedActors);
    assertTrue(trackedActors.isEmpty());
  }

  @Test
  public void shouldDrawAndSuppressOriginalUiCoverMainBranchesAndRecoverFromErrors() {
    NPC trackedNpc = mockNpc("Zebak", worldView);
    Renderable renderable = Mockito.mock(Renderable.class);
    when(trackedNpc.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));
    when(trackedNpc.getModel()).thenReturn(Mockito.mock(Model.class));

    when(config.renderLocalPlayer()).thenReturn(true);
    when(config.hideOriginalOverlays()).thenReturn(true);
    when(config.mirrorHitsplats()).thenReturn(true);
    when(config.renderNpcs()).thenReturn(true);
    when(config.npcRenderMode()).thenReturn(NpcRenderMode.CONFIGURED_LIST);
    when(config.npcNameList()).thenReturn("Zebak");
    plugin.startUp();

    assertTrue(
        TestUtils.invoke(
            plugin,
            "shouldDraw",
            new Class<?>[] {Renderable.class, boolean.class},
            renderable,
            true));
    assertFalse(
        TestUtils.invoke(
            plugin,
            "shouldDraw",
            new Class<?>[] {Renderable.class, boolean.class},
            localPlayer,
            true));
    assertFalse(
        TestUtils.invoke(
            plugin,
            "shouldDraw",
            new Class<?>[] {Renderable.class, boolean.class},
            localPlayer,
            false));
    assertTrue(
        TestUtils.invoke(
            plugin,
            "shouldDraw",
            new Class<?>[] {Renderable.class, boolean.class},
            renderable,
            false));
    assertTrue(
        TestUtils.invoke(
            plugin, "shouldSuppressOriginalUi", new Class<?>[] {Actor.class}, localPlayer));
    assertTrue(
        TestUtils.invoke(
            plugin, "shouldSuppressOriginalUi", new Class<?>[] {Actor.class}, trackedNpc));

    when(config.hideOriginalOverlays()).thenReturn(false);
    assertFalse(
        TestUtils.invoke(
            plugin, "shouldSuppressOriginalUi", new Class<?>[] {Actor.class}, trackedNpc));

    when(config.hideOriginalOverlays()).thenReturn(true);
    when(config.mirrorHitsplats()).thenReturn(false);
    when(config.mirrorHeadIcons()).thenReturn(false);
    when(config.mirrorHealthBars()).thenReturn(false);
    when(config.mirrorActorNames()).thenReturn(false);
    assertFalse(
        TestUtils.invoke(
            plugin, "shouldSuppressOriginalUi", new Class<?>[] {Actor.class}, trackedNpc));

    when(config.renderLocalPlayer()).thenThrow(new RuntimeException("boom"));
    assertTrue(
        TestUtils.invoke(
            plugin,
            "shouldDraw",
            new Class<?>[] {Renderable.class, boolean.class},
            localPlayer,
            false));
    assertTrue(
        TestUtils.invoke(
            plugin,
            "shouldDraw",
            new Class<?>[] {Renderable.class, boolean.class},
            localPlayer,
            false));
    assertTrue(TestUtils.getField(plugin, "drawListenerErrorLogged", Boolean.class));
  }

  @Test
  public void pruneMirroredHitsplatsAndTrackedActorChecksCoverRemainingBranches() {
    NPC trackedNpc = mockNpc("Zebak", worldView);
    Actor nonNpcActor = Mockito.mock(Actor.class);

    when(trackedNpc.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));
    when(trackedNpc.getModel()).thenReturn(Mockito.mock(Model.class));
    when(config.renderNpcs()).thenReturn(true);
    when(config.npcRenderMode()).thenReturn(NpcRenderMode.CONFIGURED_LIST);
    when(config.npcNameList()).thenReturn("Zebak");
    when(config.mirrorHitsplats()).thenReturn(true);
    plugin.startUp();

    getMirroredHitsplats()
        .put(trackedNpc, new ArrayList<>(List.of(TestUtils.mirroredHitsplat(1, 10, 100))));
    TestUtils.invoke(
        plugin, "pruneMirroredHitsplats", new Class<?>[] {Set.class}, Set.of(trackedNpc));
    assertTrue(getMirroredHitsplats().isEmpty());

    assertFalse(
        TestUtils.invoke(plugin, "isTrackedActor", new Class<?>[] {Actor.class}, (Actor) null));
    assertFalse(
        TestUtils.invoke(plugin, "isTrackedActor", new Class<?>[] {Actor.class}, nonNpcActor));

    when(config.renderNpcs()).thenReturn(false);
    assertFalse(
        TestUtils.invoke(plugin, "isTrackedActor", new Class<?>[] {Actor.class}, trackedNpc));
  }

  @Test
  public void provideConfigDelegatesToConfigManager() {
    ConfigManager configManager = Mockito.mock(ConfigManager.class);
    when(configManager.getConfig(TrueTileRendererConfig.class)).thenReturn(config);

    assertSame(config, plugin.provideConfig(configManager));
  }

  private IdentityHashMap<Actor, RuneLiteObject> getOutlineObjects() {
    @SuppressWarnings("unchecked")
    IdentityHashMap<Actor, RuneLiteObject> outlineObjects =
        TestUtils.getField(plugin, "outlineObjects", IdentityHashMap.class);
    return outlineObjects;
  }

  private IdentityHashMap<Actor, List<TrueTileRendererPlugin.MirroredHitsplat>>
      getMirroredHitsplats() {
    @SuppressWarnings("unchecked")
    IdentityHashMap<Actor, List<TrueTileRendererPlugin.MirroredHitsplat>> mirroredHitsplats =
        TestUtils.getField(plugin, "mirroredHitsplats", IdentityHashMap.class);
    return mirroredHitsplats;
  }

  private Set<String> getConfiguredNpcNames() {
    @SuppressWarnings("unchecked")
    Set<String> configuredNpcNames = TestUtils.getField(plugin, "configuredNpcNames", Set.class);
    return configuredNpcNames;
  }

  @SuppressWarnings("deprecation")
  private void verifyHooksRegistered() {
    verify(hooks).registerRenderableDrawListener(any());
  }

  @SuppressWarnings("deprecation")
  private void verifyHooksUnregistered() {
    verify(hooks).unregisterRenderableDrawListener(any());
  }

  private static Hitsplat mockHitsplat(int type, int amount, int disappearsOnGameCycle) {
    Hitsplat hitsplat = Mockito.mock(Hitsplat.class);
    when(hitsplat.getHitsplatType()).thenReturn(type);
    when(hitsplat.getAmount()).thenReturn(amount);
    when(hitsplat.getDisappearsOnGameCycle()).thenReturn(disappearsOnGameCycle);
    return hitsplat;
  }

  private static NPC mockNpc(String name, WorldView worldView) {
    NPC npc = Mockito.mock(NPC.class);
    NPCComposition composition = Mockito.mock(NPCComposition.class);
    when(npc.getComposition()).thenReturn(composition);
    when(composition.getName()).thenReturn(name);
    when(npc.getName()).thenReturn(name);
    when(npc.getWorldView()).thenReturn(worldView);
    when(npc.getInteracting()).thenReturn(Mockito.mock(Actor.class));
    return npc;
  }
}
