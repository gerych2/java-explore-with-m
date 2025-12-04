package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.category.NewCategoryDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CategoryMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Transactional
    public CategoryDto createCategory(NewCategoryDto dto) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new ConflictException("Category name already exists: " + dto.getName());
        }
        Category category = CategoryMapper.toEntity(dto);
        Category savedCategory = categoryRepository.save(category);
        log.info("Category created: id={}, name={}", savedCategory.getId(), savedCategory.getName());
        return CategoryMapper.toDto(savedCategory);
    }

    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto dto) {
        Category category = getCategoryById(catId);
        if (categoryRepository.existsByNameAndIdNot(dto.getName(), catId)) {
            throw new ConflictException("Category name already exists: " + dto.getName());
        }
        category.setName(dto.getName());
        Category savedCategory = categoryRepository.save(category);
        log.info("Category updated: id={}, name={}", savedCategory.getId(), savedCategory.getName());
        return CategoryMapper.toDto(savedCategory);
    }

    @Transactional
    public void deleteCategory(Long catId) {
        if (!categoryRepository.existsById(catId)) {
            throw new NotFoundException("Category with id=" + catId + " was not found");
        }
        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("The category is not empty");
        }
        categoryRepository.deleteById(catId);
        log.info("Category deleted: id={}", catId);
    }

    public List<CategoryDto> getCategories(int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        return categoryRepository.findAll(pageable).stream()
                .map(CategoryMapper::toDto)
                .collect(Collectors.toList());
    }

    public CategoryDto getCategory(Long catId) {
        Category category = getCategoryById(catId);
        return CategoryMapper.toDto(category);
    }

    public Category getCategoryById(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
    }
}

