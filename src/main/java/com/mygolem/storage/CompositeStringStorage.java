package com.mygolem.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CompositeStringStorage implements StringStorage {

    private final List<StringStorage> delegates;

    public CompositeStringStorage(List<StringStorage> delegates) {
        this.delegates = delegates == null ? List.of() : delegates.stream().filter(item -> item != null).toList();
    }

    @Override
    public List<String> addItems(Collection<String> incoming) {
        List<String> leftovers = new ArrayList<>(incoming);
        for (StringStorage delegate : delegates) {
            if (leftovers.isEmpty()) {
                break;
            }
            leftovers = delegate.addItems(leftovers);
        }
        return leftovers;
    }
}
