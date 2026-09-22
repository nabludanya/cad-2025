# Отчет о лабораторной работе №3

## Выполнение работы

### Создан новый класс CategoryFileReader

```
package ru.bsuedu.cad.lab;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Component("categoryFileReader")
public class CategoryFileReader implements Reader {

    @Value("${category.file}")
    private String filename;

    @PostConstruct
    public void init() {
        System.out.println("CategoryFileReader инициализирован: " + LocalDateTime.now());
    }

    @Override
    public List<String> read() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка чтения файла " + filename, e);
        }
    }
}
```

### Создан новый класс CategoryRequest

```
package ru.bsuedu.cad.lab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Component
public class CategoryRequest {

    private static final Logger logger = LoggerFactory.getLogger(CategoryRequest.class);
    private final DataSource dataSource;

    public CategoryRequest(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void execute() {
        String sql = """
            SELECT c.NAME, COUNT(p.ID) AS CNT
            FROM CATEGORIES c
            JOIN PRODUCTS p ON c.ID = p.CATEGORY_ID
            GROUP BY c.NAME
            HAVING COUNT(p.ID) > 1
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                logger.info("Категория: {} | Количество товаров: {}",
                        rs.getString("NAME"), rs.getInt("CNT"));
            }

        } catch (Exception e) {
            throw new RuntimeException("Ошибка запроса категорий", e);
        }
    }
}
```

### Создан новый класс ConcreteCategoryProvider

```
package ru.bsuedu.cad.lab;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConcreteCategoryProvider {

    private final Reader reader;

    public ConcreteCategoryProvider(@Qualifier("categoryFileReader") Reader reader) {
        this.reader = reader;
    }

    public List<Category> getCategories() {
        return reader.read().stream()
                .skip(1)
                .filter(line -> !line.isBlank())
                .map(line -> {
                    String[] parts = line.split(",", 3);
                    return new Category(
                            Long.parseLong(parts[0].trim()),
                            parts[1].trim(),
                            parts[2].trim()
                    );
                })
                .toList();
    }
}
```

### Создан новый класс DataBaseRenderer

```
package ru.bsuedu.cad.lab;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

@Component
@Primary
public class DataBaseRenderer implements Renderer {

    private final DataSource dataSource;
    private final ConcreteCategoryProvider categoryProvider;

    public DataBaseRenderer(DataSource dataSource, ConcreteCategoryProvider categoryProvider) {
        this.dataSource = dataSource;
        this.categoryProvider = categoryProvider;
    }

    @Override
    public void render(List<Product> products) {
        try (Connection conn = dataSource.getConnection()) {

            String sqlCat = "INSERT INTO CATEGORIES(ID, NAME, DESCRIPTION) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlCat)) {
                for (Category c : categoryProvider.getCategories()) {
                    ps.setLong(1, c.getId());
                    ps.setString(2, c.getName());
                    ps.setString(3, c.getDescription());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            String sqlProd = "INSERT INTO PRODUCTS(ID, NAME, DESCRIPTION, PRICE, STOCK_QUANTITY, CATEGORY_ID) " +
                             "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlProd)) {
                for (Product p : products) {
                    ps.setLong(1, p.getId());
                    ps.setString(2, p.getName());
                    ps.setString(3, p.getDescription());
                    ps.setDouble(4, p.getPrice());
                    ps.setInt(5, p.getStockQuantity());
                    ps.setLong(6, p.getCategoryId());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            System.out.println("Данные сохранены");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

### Сборка проекта

```
PS C:\Users\nabludanya\cross-platform\cad-2025\les06\lab> gradle run
Starting a Gradle Daemon, 4 incompatible and 3 stopped Daemons could not be reused, use --status for details
Calculating task graph as no cached configuration is available for tasks: run

> Task :app:run
CategoryFileReader инициализирован: 2026-09-22T11:09:19.082572200
ResourceFileReader инициализирован: 2026-09-22T11:09:19.382011700
Время парсинга CSV: 3 мс
Данные сохранены
11:09:19 INFO  - Категория: Средства ухода | Количество товаров: 2

BUILD SUCCESSFUL in 23s
3 actionable tasks: 2 executed, 1 up-to-date
Configuration cache entry stored.
```

## Результат работы

Обновленная диаграмма классов Mermaid

```mermaid
classDiagram
    note "Товары для магазина животных"

    class App {
        +main(String[] args)
    }

    class AppConfig {
        <<Configuration>>
        <<ComponentScan>>
        <<PropertySource>>
        <<EnableAspectJAutoProxy>>
        +dataSource() DataSource
    }

    class Product {
        -long id
        -String name
        -String description
        -double price
        -int stockQuantity
        -long categoryId
        +getId() long
        +getName() String
        +getDescription() String
        +getPrice() double
        +getStockQuantity() int
        +getCategoryId() long
    }

    class Category {
        -long id
        -String name
        -String description
        +getId() long
        +getName() String
        +getDescription() String
    }

    class Reader {
        <<interface>>
        +read() List~String~
    }

    class Parser {
        <<interface>>
        +parse(List~String~) List~Product~
    }

    class Renderer {
        <<interface>>
        +render(List~Product~) void
    }

    class ProductProvider {
        <<interface>>
        +getProducts() List~Product~
    }

    class ResourceFileReader {
        -String filename
        +init() void
        +read() List~String~
    }

    class CategoryFileReader {
        -String filename
        +init() void
        +read() List~String~
    }

    class CSVParser {
        +parse(List~String~) List~Product~
    }

    class ConcreteProductProvider {
        -Reader reader
        -Parser parser
        +getProducts() List~Product~
    }

    class ConcreteCategoryProvider {
        -Reader reader
        +getCategories() List~Category~
    }

    class ConsoleTableRenderer {
        +render(List~Product~) void
    }

    class HTMLTableRenderer {
        +render(List~Product~) void
    }

    class DataBaseRenderer {
        <<Primary>>
        -DataSource dataSource
        -ConcreteCategoryProvider categoryProvider
        +render(List~Product~) void
    }

    class CategoryRequest {
        -DataSource dataSource
        +execute() void
    }

    class TimingAspect {
        <<Aspect>>
        +measureTime(ProceedingJoinPoint) Object
    }

    Reader <|.. ResourceFileReader
    Reader <|.. CategoryFileReader
    Parser <|.. CSVParser
    ProductProvider <|.. ConcreteProductProvider
    Renderer <|.. ConsoleTableRenderer
    Renderer <|.. HTMLTableRenderer
    Renderer <|.. DataBaseRenderer

    ConcreteProductProvider --> Reader
    ConcreteProductProvider --> Parser
    ConcreteCategoryProvider --> Reader
    ConcreteCategoryProvider --> Category
    DataBaseRenderer --> ConcreteCategoryProvider
    DataBaseRenderer --> Product
    DataBaseRenderer --> Category
    CategoryRequest --> DataSource
    DataBaseRenderer --> DataSource
    TimingAspect ..> CSVParser : @Around

    App --> ProductProvider
    App --> Renderer
    App --> CategoryRequest
    App --> AppConfig
```