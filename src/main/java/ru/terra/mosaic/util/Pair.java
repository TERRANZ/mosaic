package ru.terra.mosaic.util;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Pair<K, V> {
    private final K key;
    private final V value;
}
