package com.truetilerenderer;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.Player;

final class TrueTileActorMatcher
{
	private TrueTileActorMatcher()
	{
	}

	static Set<String> parseConfiguredNpcNames(String rawNames)
	{
		if (rawNames == null || rawNames.isBlank())
		{
			return Collections.emptySet();
		}

		return rawNames.lines()
			.flatMap(line -> Arrays.stream(line.split(",")))
			.map(String::trim)
			.filter(name -> !name.isEmpty())
			.map(TrueTileActorMatcher::normalizeName)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	static boolean shouldRenderNpc(NPC npc, Player localPlayer, NpcRenderMode renderMode, Set<String> configuredNpcNames)
	{
		if (npc == null || localPlayer == null || npc.getWorldLocation() == null || npc.getModel() == null)
		{
			return false;
		}

		if (renderMode == NpcRenderMode.CURRENT_TARGET)
		{
			Actor interacting = localPlayer.getInteracting();
			return interacting == npc || npc.getInteracting() == localPlayer;
		}

		String npcName = getNpcName(npc);
		return npcName != null && configuredNpcNames.contains(normalizeName(npcName));
	}

	static String getNpcName(NPC npc)
	{
		if (npc.getTransformedComposition() != null && npc.getTransformedComposition().getName() != null)
		{
			return npc.getTransformedComposition().getName();
		}

		if (npc.getComposition() != null)
		{
			return npc.getComposition().getName();
		}

		return npc.getName();
	}

	static String normalizeName(String name)
	{
		return name.toLowerCase(Locale.ROOT);
	}
}
