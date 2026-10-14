package com.studentfinance.service;

import com.studentfinance.dto.CategoryResponse;
import com.studentfinance.entity.Category;
import com.studentfinance.entity.TransactionType;
import com.studentfinance.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll(TransactionType type) {
        List<Category> categories = (type != null)
                ? categoryRepository.findByType(type)
                : categoryRepository.findAll();

        return categories.stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getType()))
                .collect(Collectors.toList());
    }
}
