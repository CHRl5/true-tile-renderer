package com.truetilerenderer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import net.runelite.api.Actor;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import org.mockito.Mockito;

public class TrueTileActorMatcherTest {
  @Test
  public void parseConfiguredNpcNamesNormalizesAndDeduplicates() {
    Set<String> parsed =
        TrueTileActorMatcher.parseConfiguredNpcNames(" Zebak, WardeN \n zebak\n\n");
    assertEquals(Set.of("zebak", "warden"), parsed);
  }

  @Test
  public void parseConfiguredNpcNamesReturnsEmptySetForBlankInput() {
    assertEquals(Set.of(), TrueTileActorMatcher.parseConfiguredNpcNames(null));
    assertEquals(Set.of(), TrueTileActorMatcher.parseConfiguredNpcNames(" \n "));
  }

  @Test
  public void currentTargetModeMatchesLocalPlayerInteraction() {
    Player localPlayer = Mockito.mock(Player.class);
    NPC npc = mockNpc("Zebak");
    Mockito.when(localPlayer.getInteracting()).thenReturn(npc);

    assertTrue(
        TrueTileActorMatcher.shouldRenderNpc(
            npc, localPlayer, NpcRenderMode.CURRENT_TARGET, Set.of()));
  }

  @Test
  public void currentTargetModeMatchesNpcInteractionWithLocalPlayer() {
    Player localPlayer = Mockito.mock(Player.class);
    NPC npc = mockNpc("Zebak");
    Mockito.when(npc.getInteracting()).thenReturn(localPlayer);

    assertTrue(
        TrueTileActorMatcher.shouldRenderNpc(
            npc, localPlayer, NpcRenderMode.CURRENT_TARGET, Set.of()));
  }

  @Test
  public void currentTargetModeRejectsUntrackedOrIncompleteNpc() {
    Player localPlayer = Mockito.mock(Player.class);
    NPC npc = mockNpc("Zebak");
    NPC nullWorldNpc = mockNpc("Zebak");
    NPC nullModelNpc = mockNpc("Zebak");

    Mockito.when(localPlayer.getInteracting()).thenReturn(Mockito.mock(Actor.class));
    Mockito.when(nullWorldNpc.getWorldLocation()).thenReturn(null);
    Mockito.when(nullModelNpc.getModel()).thenReturn(null);

    assertFalse(
        TrueTileActorMatcher.shouldRenderNpc(
            null, localPlayer, NpcRenderMode.CURRENT_TARGET, Set.of()));
    assertFalse(
        TrueTileActorMatcher.shouldRenderNpc(npc, null, NpcRenderMode.CURRENT_TARGET, Set.of()));
    assertFalse(
        TrueTileActorMatcher.shouldRenderNpc(
            npc, localPlayer, NpcRenderMode.CURRENT_TARGET, Set.of()));
    assertFalse(
        TrueTileActorMatcher.shouldRenderNpc(
            nullWorldNpc, localPlayer, NpcRenderMode.CURRENT_TARGET, Set.of()));
    assertFalse(
        TrueTileActorMatcher.shouldRenderNpc(
            nullModelNpc, localPlayer, NpcRenderMode.CURRENT_TARGET, Set.of()));
  }

  @Test
  public void configuredListModeMatchesNormalizedNpcName() {
    Player localPlayer = Mockito.mock(Player.class);
    NPC npc = mockNpc("The Whisperer");

    assertTrue(
        TrueTileActorMatcher.shouldRenderNpc(
            npc, localPlayer, NpcRenderMode.CONFIGURED_LIST, Set.of("the whisperer")));
  }

  @Test
  public void configuredListModeMatchesTransformedNpcName() {
    Player localPlayer = Mockito.mock(Player.class);
    NPC npc = mockNpc("Kephri", "Scarab Swarm");

    assertTrue(
        TrueTileActorMatcher.shouldRenderNpc(
            npc, localPlayer, NpcRenderMode.CONFIGURED_LIST, Set.of("scarab swarm")));
  }

  @Test
  public void getNpcNameFallsBackThroughAvailableSources() {
    NPC transformedNpc = mockNpc("Kephri", "Scarab Swarm");
    NPC compositionNpc = mockNpc("Vardorvis");
    NPC bareNpc = Mockito.mock(NPC.class);

    Mockito.when(bareNpc.getName()).thenReturn("The Leviathan");

    assertEquals("Scarab Swarm", TrueTileActorMatcher.getNpcName(transformedNpc));
    assertEquals("Vardorvis", TrueTileActorMatcher.getNpcName(compositionNpc));
    assertEquals("The Leviathan", TrueTileActorMatcher.getNpcName(bareNpc));
  }

  @Test
  public void configuredListModeRejectsUnknownNpc() {
    Player localPlayer = Mockito.mock(Player.class);
    NPC npc = mockNpc("Vardorvis");

    assertFalse(
        TrueTileActorMatcher.shouldRenderNpc(
            npc, localPlayer, NpcRenderMode.CONFIGURED_LIST, Set.of("duke sucellus")));
  }

  @Test
  public void normalizeNameUsesRootLocaleLowercasing() {
    assertEquals("the whisperer", TrueTileActorMatcher.normalizeName("The Whisperer"));
  }

  private static NPC mockNpc(String name) {
    return mockNpc(name, null);
  }

  private static NPC mockNpc(String name, String transformedName) {
    NPC npc = Mockito.mock(NPC.class);
    NPCComposition composition = Mockito.mock(NPCComposition.class);
    NPCComposition transformedComposition = Mockito.mock(NPCComposition.class);
    Model model = Mockito.mock(Model.class);
    WorldPoint worldPoint = new WorldPoint(3200, 3200, 0);

    Mockito.when(npc.getComposition()).thenReturn(composition);
    Mockito.when(composition.getName()).thenReturn(name);
    Mockito.when(npc.getTransformedComposition())
        .thenReturn(transformedName == null ? null : transformedComposition);
    if (transformedName != null) {
      Mockito.when(transformedComposition.getName()).thenReturn(transformedName);
    }
    Mockito.when(npc.getName()).thenReturn(name);
    Mockito.when(npc.getModel()).thenReturn(model);
    Mockito.when(npc.getWorldLocation()).thenReturn(worldPoint);
    Mockito.when(npc.getInteracting()).thenReturn(Mockito.mock(Actor.class));
    return npc;
  }
}
