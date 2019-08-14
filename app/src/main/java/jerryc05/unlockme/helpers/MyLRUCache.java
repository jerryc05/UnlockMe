package jerryc05.unlockme.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A LinkedHashMap implementation of LRU cache.
 *
 * @author \
 * \           d88b d88888b d8888b. d8888b. db    db  .o88b.  .d88b.    ooooo
 * \           `8P' 88'     88  `8D 88  `8D `8b  d8' d8P  Y8 .8P  88.  8P~~~~
 * \            88  88ooooo 88oobY' 88oobY'  `8bd8'  8P      88  d'88 dP
 * \            88  88~~~~~ 88`8b   88`8b      88    8b      88 d' 88 V8888b.
 * \        db. 88  88.     88 `88. 88 `88.    88    Y8b  d8 `88  d8'     `8D
 * \        Y8888P  Y88888P 88   YD 88   YD    YP     `Y88P'  `Y88P'  88oobY'
 * @see java.util.LinkedHashMap
 */
@SuppressWarnings("unused")
public class MyLRUCache<K, V> implements Map<K, V> {

  private final LinkedHashMap<K, V> map;

  /**
   * Size of this cache in units. Not necessarily the number of elements.
   */
  private int size;
  private int maxSize;

  /**
   * @param maxSize for caches that do not override {@link #sizeOf}, this is
   *                the maximum number of entries in the cache. For all other caches,
   *                this is the maximum sum of the sizes of the entries in this cache.
   */
  public MyLRUCache(int maxSize) {
    this(maxSize, 1, .75f, true);
  }

  @SuppressWarnings("WeakerAccess")
  public MyLRUCache(int maxSize, int initialCapacity, float loadFactor, boolean accessOrder) {
    if (maxSize <= 0)
      throw new IllegalArgumentException("maxSize <= 0");
    this.maxSize = maxSize;
    this.map = new LinkedHashMap<>(initialCapacity, loadFactor, accessOrder);
  }

  public final void resize(int maxSize) {
    if (maxSize <= 0)
      throw new IllegalArgumentException("maxSize <= 0");

    synchronized (this) {
      this.maxSize = maxSize;
    }
    trimToSize(maxSize);
  }

  public final V get(@Nullable final Object key) {
    if (key == null)
      throw new NullPointerException("key == null");

    synchronized (map) {
      return map.get(key);
    }
  }

  /**
   * Caches {@code value} for {@code key}. The value is moved to the head of
   * the queue.
   *
   * @return the previous value mapped by {@code key}.
   */
  public final V put(@NonNull final K key, @NonNull final V value) {
    final V previous;

    synchronized (this) {
      size += safeSizeOf(key, value);
      previous = map.put(key, value);
      if (previous != null)
        size -= safeSizeOf(key, previous);
    }

    if (previous != null)
      onRemoveEntry(false, key, previous, value);

    trimToSize(maxSize);
    return previous;
  }

  /**
   * @param maxSize the maximum size of the cache before returning. May be -1
   *                to evict even 0-sized elements.
   */
  private void trimToSize(int maxSize) {
    while (true) {
      Map.Entry<K, V> toEvict = null;

      synchronized (this) {
        if (size < 0 || (map.isEmpty() && size != 0))
          throw new IllegalStateException(getClass().getName()
                  + ".sizeOf() is reporting inconsistent results!");

        if (size <= maxSize) break;

        // BEGIN LAYOUTLIB CHANGE
        // get the last item in the linked list.
        // This is not efficient, the goal here is to minimize the changes
        // compared to the platform version.
        for (Map.Entry<K, V> entry : map.entrySet()) {
          toEvict = entry;
        }
        // END LAYOUTLIB CHANGE

        if (toEvict == null) break;

        map.remove(toEvict.getKey());
        size -= safeSizeOf(toEvict.getKey(), toEvict.getValue());
      }
      onRemoveEntry(true, toEvict.getKey(), toEvict.getValue(), null);
    }
  }

  /**
   * Removes the entry for {@code key} if it exists.
   *
   * @return the previous value mapped by {@code key}.
   */
  @SuppressWarnings("unchecked")
  public final V remove(@Nullable final Object key) {
    if (key == null)
      throw new NullPointerException("key == null");

    final V previous;
    synchronized (this) {
      previous = map.remove(key);
      if (previous != null)
        size -= safeSizeOf((K) key, previous);
    }

    if (previous != null)
      onRemoveEntry(false, (K) key, previous, null);

    return previous;
  }

  /**
   * Called for entries that have been evicted or removed. This method is
   * invoked when a value is evicted to make space, removed by a call to
   * {@link #remove}, or replaced by a call to {@link #put}. The default
   * implementation does nothing.
   *
   * <p>The method is called without synchronization: other threads may
   * access the cache while this method is executing.
   *
   * @param evicted  true if the entry is being removed to make space, false
   *                 if the removal was caused by a {@link #put} or {@link #remove}.
   * @param newValue the new value for {@code key}, if it exists. If non-null,
   *                 this removal was caused by a {@link #put}. Otherwise it was caused by
   *                 an eviction or a {@link #remove}.
   */
  @Deprecated
  protected void entryRemoved(boolean evicted, @NonNull final K key,
                              @NonNull final V oldValue, @Nullable final V newValue) {
  }

  @SuppressWarnings("WeakerAccess")
  protected void onRemoveEntry(boolean evicted, @NonNull final K key,
                               @NonNull final V oldValue, @Nullable final V newValue) {
  }

  private int safeSizeOf(@NonNull final K key, @NonNull final V value) {
    int result = sizeOf(key, value);
    if (result < 0)
      throw new IllegalStateException("Negative size: " + key + "=" + value);
    return result;
  }

  /**
   * Returns the size of the entry for {@code key} and {@code value} in
   * user-defined units.  The default implementation returns 1 so that size
   * is the number of entries and max size is the maximum number of entries.
   *
   * <p>An entry's size must not change while it is in the cache.
   */
  @SuppressWarnings("WeakerAccess")
  protected int sizeOf(@NonNull final K key, @NonNull final V value) {
    return 1;
  }

  /**
   * Clear the cache, calling {@link #onRemoveEntry} on each removed entry.
   */
  @Deprecated
  public final void evictAll() {
    trimToSize(-1); // -1 will evict 0-sized elements
  }

  @Override
  public void clear() {
    trimToSize(-1); // -1 will evict 0-sized elements
  }

  public void clearNow() {
    map.clear();
    size = 0;
  }

  /**
   * For caches that do not override {@link #sizeOf}, this returns the number
   * of entries in the cache. For all other caches, this returns the sum of
   * the sizes of the entries in this cache.
   */
  @Deprecated
  public synchronized final int size() {
    return size;
  }

  public synchronized final int getSize() {
    return size;
  }

  /**
   * For caches that do not override {@link #sizeOf}, this returns the maximum
   * number of entries in the cache. For all other caches, this returns the
   * maximum sum of the sizes of the entries in this cache.
   */
  @Deprecated
  public synchronized final int maxSize() {
    return maxSize;
  }

  public synchronized final int getMaxSize() {
    return maxSize;
  }

  /**
   * Returns a copy of the current contents of the cache, ordered from least
   * recently accessed to most recently accessed.
   */
  public synchronized final LinkedHashMap<K, V> snapshot() {
    return new LinkedHashMap<>(map);
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
    return map.values();
  }

  @NonNull
  @Override
  public Set<Entry<K, V>> entrySet() {
    return map.entrySet();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public boolean containsKey(@Nullable final Object o) {
    return map.containsKey(o);
  }

  @Override
  public boolean containsValue(@Nullable final Object o) {
    return map.containsValue(o);
  }
}
