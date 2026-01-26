package org.viators.personal_finance_app.dto.store.response;

import org.viators.personal_finance_app.model.Store;

import java.util.Set;
import java.util.stream.Collectors;

public record StoreSummary(
        String uuid,
        Long id,
        String name
) {
    public static StoreSummary from(Store store) {
        return new StoreSummary(store.getUuid(), store.getId(), store.getName());
    }

    public static Set<StoreSummary> fromList(Set<Store> stores) {
        return stores.stream()
                .map(StoreSummary::from)
                .collect(Collectors.toSet());
    }
}
