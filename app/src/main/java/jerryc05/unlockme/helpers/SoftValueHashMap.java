package jerryc05.unlockme.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class SoftValueHashMap<K, V> implements Map<K, V> {

  private final HashMap<K, SoftReference<V>> map;

  public SoftValueHashMap(int initialCapacity, float loadFactor) {
    map = new HashMap<>(initialCapacity, loadFactor);
  }

  @Nullable
  @Override
  public V get(@Nullable final Object o) {
    final SoftReference<V> sr = map.get(o);
    if (sr == null) return null;
    final V val = sr.get();
    if (val == null) //noinspection SuspiciousMethodCalls
      map.remove(o);
    return val;
  }

  @Nullable
  @Override
  public V put(@NonNull final K k, @NonNull final V v) {
    final V old = get(k);
    map.put(k, new SoftReference<>(v));
    return old;
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Nullable
  @Override
  public V remove(@Nullable final Object o) {
    final V old = get(o);
    map.remove(o);
    return old;
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public boolean containsKey(@Nullable final Object o) {
    return map.containsKey(o);
  }

  @Override
  public boolean containsValue(@Nullable final Object o) {
    throw new UnsupportedOperationException("Don't call containsValue()!");
  }

  @Override
  public void putAll(@NonNull final Map<? extends K, ? extends V> map) {
    for (Entry<? extends K, ? extends V> entry : map.entrySet())
      put(entry.getKey(), entry.getValue());
  }

  @NonNull
  @Override
  public Set<K> keySet() {
    return map.keySet();
  }

  @NonNull
  @Override
  public Collection<V> values() {
    throw new UnsupportedOperationException("Don't call values()!");
  }

  @NonNull
  @Override
  public Set<Entry<K, V>> entrySet() {
    throw new UnsupportedOperationException("Don't call entrySet()!");
  }
}
