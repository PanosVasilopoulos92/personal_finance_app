package org.viators.personalfinanceapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.viators.personalfinanceapp.dto.shoppinglist.request.CreateShoppingListRequest;
import org.viators.personalfinanceapp.dto.shoppinglist.request.UpdateShoppingListRequest;
import org.viators.personalfinanceapp.dto.shoppinglist.response.ShoppingListSummaryResponse;
import org.viators.personalfinanceapp.exceptions.ResourceNotFoundException;
import org.viators.personalfinanceapp.model.ShoppingList;
import org.viators.personalfinanceapp.model.User;
import org.viators.personalfinanceapp.model.enums.StatusEnum;
import org.viators.personalfinanceapp.repository.ShoppingListRepository;
import org.viators.personalfinanceapp.repository.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShoppingListService {

    private final UserRepository userRepository;
    private final ShoppingListRepository shoppingListRepository;

    @Transactional
    public ShoppingListSummaryResponse create(String userUuid, CreateShoppingListRequest request) {
        User user = userRepository.findByUuidAndStatus(userUuid, StatusEnum.ACTIVE.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("User does not exist system"));

        ShoppingList shoppingList = request.toEntity();
        shoppingList = shoppingListRepository.save(shoppingList);
        return ShoppingListSummaryResponse.from(shoppingList);
    }

    @Transactional
    public ShoppingListSummaryResponse update(String uuid, UpdateShoppingListRequest request) {
        ShoppingList shoppingList = shoppingListRepository.findByUuidAndStatus(uuid, StatusEnum.ACTIVE.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("Shopping list not found or is inactive"));

        request.update(shoppingList);
        return ShoppingListSummaryResponse.from(shoppingList);
    }

    public ShoppingListSummaryResponse addListItem() {

    }
}
