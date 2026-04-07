package com.truetilerenderer;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TrueTileRendererPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TrueTileRendererPlugin.class);
		RuneLite.main(args);
	}
}
