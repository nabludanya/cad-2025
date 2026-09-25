package ru.bsuedu.cad.lab.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import ru.bsuedu.cad.lab.entity.*;
import ru.bsuedu.cad.lab.repository.*;

import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class DataLoaderService {

    private static final Logger logger = LoggerFactory.getLogger(DataLoaderService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final CategoryRepository categoryRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    private final Map<Long, Category> categoryMap = new HashMap<>();
    private final Map<Long, Customer> customerMap = new HashMap<>();

    public DataLoaderService(CategoryRepository categoryRepository,
                             CustomerRepository customerRepository,
                             ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }
    
    @PostConstruct
    @Transactional
    public void loadAllData() {
        logger.info("Загрузка CSV-файлов...");
        loadCategories();
        loadCustomers();
        loadProducts();
        logger.info("Загрузка данных успешно выполнена");
    }

    @Transactional
    public void loadCategories() {
        logger.info("Загрузка категорий...");
        try (Reader reader = new InputStreamReader(
                new ClassPathResource("data/category.csv").getInputStream(),
                StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                Long id = Long.parseLong(record.get("category_id"));
                String name = record.get("name");
                String description = record.get("description");

                Category category = new Category(name, description);
                Category savedCategory = categoryRepository.save(category);
                categoryMap.put(id, savedCategory);
            }
            logger.info("Загружено {} категорий", categoryMap.size());
        } catch (Exception e) {
            logger.error("Ошибка загрузки категорий", e);
            throw new RuntimeException("Failed to load categories", e);
        }
    }

    @Transactional
    public void loadCustomers() {
        logger.info("Загрузка покупателей...");
        try (Reader reader = new InputStreamReader(
                new ClassPathResource("data/customer.csv").getInputStream(),
                StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                Long id = Long.parseLong(record.get("customer_id"));
                String name = record.get("name");
                String email = record.get("email");
                String phone = record.get("phone");
                String address = record.get("address");

                Customer customer = new Customer(name, email, phone, address);
                Customer savedCustomer = customerRepository.save(customer);
                customerMap.put(id, savedCustomer);
            }
            logger.info("Загружено {} покупателей", customerMap.size());
        } catch (Exception e) {
            logger.error("Ошибка загрузки покупателей", e);
            throw new RuntimeException("Failed to load customers", e);
        }
    }

    @Transactional
    public void loadProducts() {
        logger.info("Загрузка товаров...");
        try (Reader reader = new InputStreamReader(
                new ClassPathResource("data/product.csv").getInputStream(),
                StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim()
                     .parse(reader)) {

            int count = 0;
            for (CSVRecord record : parser) {
                try {
                    String name = record.get("name");
                    String description = record.get("description");
                    Long categoryId = Long.parseLong(record.get("category_id"));
                    BigDecimal price = new BigDecimal(record.get("price"));
                    Integer stockQuantity = Integer.parseInt(record.get("stock_quantity"));
                    String imageUrl = record.get("image_url");

                    LocalDateTime createdAt = null;
                    LocalDateTime updatedAt = null;

                    try {
                        String createdAtStr = record.get("created_at");
                        if (createdAtStr != null && !createdAtStr.isEmpty()) {
                            createdAt = LocalDate.parse(createdAtStr, DATE_FORMATTER).atStartOfDay();
                        }
                    } catch (Exception ignored) {}

                    try {
                        String updatedAtStr = record.get("updated_at");
                        if (updatedAtStr != null && !updatedAtStr.isEmpty()) {
                            updatedAt = LocalDate.parse(updatedAtStr, DATE_FORMATTER).atStartOfDay();
                        }
                    } catch (Exception ignored) {}

                    Category category = categoryMap.get(categoryId);
                    if (category == null) {
                        throw new RuntimeException("Категория не найдена: " + categoryId);
                    }

                    Product product = new Product(name, description, category, price, stockQuantity, imageUrl);
                    if (createdAt != null) product.setCreatedAt(createdAt);
                    if (updatedAt != null) product.setUpdatedAt(updatedAt);

                    productRepository.save(product);
                    count++;
                } catch (Exception e) {
                    logger.error("Ошибка записи товара: {}", record);
                    throw e;
                }
            }
            logger.info("Записано {} продуктов", count);
        } catch (Exception e) {
            logger.error("Ошибка загрузки продуктов", e);
            throw new RuntimeException("Failed to load products", e);
        }
    }

    public Customer getCustomerByOriginalId(Long originalId) {
        return customerMap.get(originalId);
    }

    public Category getCategoryByOriginalId(Long originalId) {
        return categoryMap.get(originalId);
    }
}