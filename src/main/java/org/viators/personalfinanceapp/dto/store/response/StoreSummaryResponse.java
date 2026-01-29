package org.viators.personalfinanceapp.dto.store.response;

import org.viators.personalfinanceapp.model.Store;

import java.util.Set;
import java.util.stream.Collectors;

public record StoreSummaryResponse(
        String uuid,
        Long id,
        String name
) {
    public static StoreSummaryResponse from(Store store) {
        return new StoreSummaryResponse(store.getUuid(), store.getId(), store.getName());
    }

    public static Set<StoreSummaryResponse> fromList(Set<Store> stores) {
        return stores.stream()
                .map(StoreSummaryResponse::from)
                .collect(Collectors.toSet());
    }
}
