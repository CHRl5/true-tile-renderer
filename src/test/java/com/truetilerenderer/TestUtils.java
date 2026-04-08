package com.truetilerenderer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.client.game.SpriteManager;
import org.mockito.Mockito;

final class TestUtils {
  private TestUtils() {}

  @SuppressFBWarnings(
      value = "DP_DO_INSIDE_DO_PRIVILEGED",
      justification = "Test-only reflection helper for private plugin internals.")
  static void setField(Object target, String fieldName, Object value) {
    try {
      Field field = findField(target.getClass(), fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException ex) {
      throw new AssertionError("Failed to set field " + fieldName, ex);
    }
  }

  @SuppressFBWarnings(
      value = "DP_DO_INSIDE_DO_PRIVILEGED",
      justification = "Test-only reflection helper for private plugin internals.")
  @SuppressWarnings("unchecked")
  static <T> T getField(Object target, String fieldName, Class<T> type) {
    try {
      Class<?> targetClass = target instanceof Class ? (Class<?>) target : target.getClass();
      Field field = findField(targetClass, fieldName);
      field.setAccessible(true);
      return (T) field.get(target instanceof Class ? null : target);
    } catch (ReflectiveOperationException ex) {
      throw new AssertionError("Failed to read field " + fieldName, ex);
    }
  }

  @SuppressFBWarnings(
      value = "DP_DO_INSIDE_DO_PRIVILEGED",
      justification = "Test-only reflection helper for private plugin internals.")
  @SuppressWarnings("unchecked")
  static <T> T invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
    try {
      Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
      method.setAccessible(true);
      return (T) method.invoke(target, args);
    } catch (ReflectiveOperationException ex) {
      throw new AssertionError("Failed to invoke method " + methodName, ex);
    }
  }

  @SafeVarargs
  static <T> IndexedObjectSet<T> indexedSetOf(T... elements) {
    List<T> values = Arrays.asList(elements);
    @SuppressWarnings("unchecked")
    IndexedObjectSet<T> indexedSet = Mockito.mock(IndexedObjectSet.class);
    Mockito.when(indexedSet.iterator()).thenAnswer(ignored -> values.iterator());
    Mockito.when(indexedSet.byIndex(Mockito.anyInt()))
        .thenAnswer(invocation -> values.get(invocation.getArgument(0)));
    return indexedSet;
  }

  static TrueTileRendererPlugin.MirroredHitsplat mirroredHitsplat(
      int hitsplatType, int amount, int disappearsOnGameCycle) {
    try {
      Constructor<TrueTileRendererPlugin.MirroredHitsplat> constructor =
          TrueTileRendererPlugin.MirroredHitsplat.class.getDeclaredConstructor(
              int.class, int.class, int.class);
      constructor.setAccessible(true);
      return constructor.newInstance(hitsplatType, amount, disappearsOnGameCycle);
    } catch (ReflectiveOperationException ex) {
      throw new AssertionError("Failed to create mirrored hitsplat", ex);
    }
  }

  static TrueTileRendererOverlay newOverlay(
      net.runelite.api.Client client,
      TrueTileRendererPlugin plugin,
      TrueTileRendererConfig config,
      SpriteManager spriteManager) {
    try {
      Constructor<TrueTileRendererOverlay> constructor =
          TrueTileRendererOverlay.class.getDeclaredConstructor(
              net.runelite.api.Client.class,
              TrueTileRendererPlugin.class,
              TrueTileRendererConfig.class,
              SpriteManager.class);
      constructor.setAccessible(true);
      return constructor.newInstance(client, plugin, config, spriteManager);
    } catch (ReflectiveOperationException ex) {
      throw new AssertionError("Failed to create overlay", ex);
    }
  }

  static Player mockPlayer(WorldView worldView) {
    Player player = Mockito.mock(Player.class);
    Mockito.when(player.getWorldView()).thenReturn(worldView);
    return player;
  }

  private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
    Class<?> current = type;
    while (current != null) {
      try {
        return current.getDeclaredField(fieldName);
      } catch (NoSuchFieldException ignored) {
        current = current.getSuperclass();
      }
    }
    throw new NoSuchFieldException(fieldName);
  }
}
