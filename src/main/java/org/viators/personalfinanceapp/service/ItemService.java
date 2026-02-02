package org.viators.personalfinanceapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.viators.personalfinanceapp.dto.item.request.CreateItemRequest;
import org.viators.personalfinanceapp.dto.item.request.UpdateItemPrice;
import org.viators.personalfinanceapp.dto.item.request.UpdateItemRequest;
import org.viators.personalfinanceapp.dto.item.response.ItemDetailsResponse;
import org.viators.personalfinanceapp.dto.item.response.ItemSummaryResponse;
import org.viators.personalfinanceapp.exceptions.BusinessException;
import org.viators.personalfinanceapp.exceptions.ResourceNotFoundException;
import org.viators.personalfinanceapp.model.*;
import org.viators.personalfinanceapp.model.enums.StatusEnum;
import org.viators.personalfinanceapp.repository.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ItemService {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final PriceObservationRepository priceObservationRepository;


    public ItemDetailsResponse getItem(String uuid) {
        Item item = itemRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Item does not exist"));

        return ItemDetailsResponse.from(item);
    }

    public Page<ItemDetailsResponse> getAllItemsForUser(String userUuid, Pageable pageable) {
        Page<Item> results = itemRepository.findAllByUser(userUuid, StatusEnum.ACTIVE.getCode(), pageable);

        return results.map(ItemDetailsResponse::from);
    }

    @Transactional
    public ItemSummaryResponse create(String userUuid, CreateItemRequest request) {
        User user = userRepository.findByUuidAndStatus(userUuid, StatusEnum.ACTIVE.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("No such user in system"));

        Store store = storeRepository.findByName(request.storeName())
                .orElseThrow(() -> new ResourceNotFoundException("Store could not be found"));

        Item item = request.toEntity();
        item.setUser(user);

        PriceObservation priceObservation = request.createPriceObservationRequest().toEntity();
        priceObservation.setStore(store);
        item.addPriceObservation(priceObservation);

        return ItemSummaryResponse.from(itemRepository.save(item));
    }

    @Transactional
    public ItemSummaryResponse update(String userUuid, UpdateItemRequest request) {
        User user = userRepository.findByUuidAndStatus(userUuid, StatusEnum.ACTIVE.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("No such user in system"));

        Item item = itemRepository.findByUuid(request.itemUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));

        if (item.getUser() != user) {
            throw new AccessDeniedException("User can update items that belong to him/her only");
        }

        if (request.categoryUuid() != null) {
            Category category = categoryRepository.findByUuid(request.categoryUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

            if (itemRepository.existsByNameAndUser_IdAndStatusAndCategoriesContaining(request.newName(),
                    user.getId(), StatusEnum.ACTIVE.getCode(), category)) {
                throw new BusinessException("Categories cannot contain items with same name");
            }
        } else {
            if (itemRepository.existsByNameAndUser_IdAndStatusAndCategoriesIsEmpty(request.newName(),
                    user.getId(), StatusEnum.ACTIVE.getCode())) {
                throw new BusinessException("User's items cannot have same name unless they belong to different categories");
            }
        }

        request.updateItem(item);

        return ItemSummaryResponse.from(item);
    }

    @Transactional
    public ItemSummaryResponse updatePrice(String userUuid, UpdateItemPrice request) {
        User user = userRepository.findByUuidAndStatus(userUuid, StatusEnum.ACTIVE.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("No such user in system"));

        Item item = itemRepository.findByUuid(request.uuid())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));

        if (item.getUser() != user) {
            throw new AccessDeniedException("User can only update items that belong to him/her");
        }

        PriceObservation lastPriceObservation = priceObservationRepository.findLastActivePriceObservation(item.getUuid(), StatusEnum.ACTIVE.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("No active price found for this item"));
        lastPriceObservation.setStatus(StatusEnum.INACTIVE.getCode());

        PriceObservation newPriceObservation = request.createPriceObservationRequest().toEntity();
        item.addPriceObservation(newPriceObservation);

        return ItemSummaryResponse.from(item);
    }

    @Transactional
    public void deactivateItem(String uuid) {
        Item item = itemRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));

        item.setStatus(StatusEnum.INACTIVE.getCode());
    }

}
