package com.awin.merchantdashboard.todo.controller;

import com.awin.merchantdashboard.api.TodosApi;
import com.awin.merchantdashboard.api.model.TodoList;
import com.awin.merchantdashboard.merchant.service.MerchantAccessGuard;
import com.awin.merchantdashboard.todo.service.TodoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TodosController implements TodosApi {

    private final MerchantAccessGuard guard;
    private final TodoService service;

    TodosController(MerchantAccessGuard guard, TodoService service) {
        this.guard = guard;
        this.service = service;
    }

    @Override
    public ResponseEntity<TodoList> getTodos(Long merchantId) {
        long userId = Long.parseLong(
                SecurityContextHolder.getContext().getAuthentication().getName());
        guard.check(userId, merchantId);
        return ResponseEntity.ok(service.todos(merchantId));
    }
}
