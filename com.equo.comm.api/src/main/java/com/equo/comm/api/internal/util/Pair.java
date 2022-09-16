package com.equo.comm.api.internal.util;

/**
 * Generic pair.
 * @param <T1> first
 * @param <T2> second
 */
public class Pair<T1, T2> {
  T1 first;
  T2 second;

  public Pair(T1 first, T2 second) {
    this.first = first;
    this.second = second;
  }

  public T1 getFirst() {
    return first;
  }

  public T2 getSecond() {
    return second;
  }
}
