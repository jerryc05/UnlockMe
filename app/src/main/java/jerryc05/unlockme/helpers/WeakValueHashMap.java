package jerryc05.unlockme.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public final class WeakValueHashMap<K, V> implements Map<K, V> {

  private final HashMap<K, WeakReference<V>> map;

  public WeakValueHashMap(int initialCapacity, float loadFactor) {
    map = new HashMap<>(initialCapacity, loadFactor);
  }

  @Nullable
  @Override
  public V get(@Nullable final Object o) {
    final WeakReference<V> wr = map.get(o);
    if (wr == null) return null;
    final V val = wr.get();
    if (val == null) //noinspection SuspiciousMethodCalls
      map.remove(o);
    return val;
  }

  @Nullable
  @Override
  public V put(@NonNull final K k, @NonNull final V v) {
    final V old = get(k);
    map.put(k, new WeakReference<>(v));
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
