package com.dartcodes.smartorder.config;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.dartcodes.smartorder.model.*;
import com.dartcodes.smartorder.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final BranchInventoryRepository inventoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded, skipping.");
            return;
        }

        log.info("Seeding database with initial data...");

        User admin = userRepository.save(User.builder()
                .name("System Admin")
                .email("admin@smartorder.com")
                .password(passwordEncoder.encode("Admin@123"))
                .role("ADMIN")
                .build());
        log.info("Created admin: admin@smartorder.com / Admin@123");

        User customer = userRepository.save(User.builder()
                .name("John Perera")
                .email("john@example.com")
                .password(passwordEncoder.encode("Customer@123"))
                .role("CUSTOMER")
                .phone("0771234567")
                .address("42 Galle Road, Colombo 03")
                .latitude(6.9271)
                .longitude(79.8612)
                .build());
        log.info("Created customer: john@example.com / Customer@123");

        Branch colombo = branchRepository.save(Branch.builder()
                .name("Colombo Main Branch")
                .address("123 Main Street, Colombo 01")
                .latitude(6.9271)
                .longitude(79.8612)
                .maxWorkload(50)
                .build());

        Branch kandy = branchRepository.save(Branch.builder()
                .name("Kandy City Branch")
                .address("45 Dalada Veediya, Kandy")
                .latitude(7.2906)
                .longitude(80.6337)
                .maxWorkload(40)
                .build());

        Branch galle = branchRepository.save(Branch.builder()
                .name("Galle Fort Branch")
                .address("78 Church Street, Galle Fort")
                .latitude(6.0328)
                .longitude(80.2170)
                .maxWorkload(30)
                .build());

        log.info("Created 3 branches: Colombo, Kandy, Galle");

        Product laptop = productRepository.save(Product.builder()
                .name("Laptop Pro 15")
                .description("15-inch professional laptop with 16GB RAM and 512GB SSD")
                .price(new BigDecimal("185000.00"))
                .category("Electronics")
                .build());

        Product phone = productRepository.save(Product.builder()
                .name("Smartphone X12")
                .description("6.5-inch AMOLED display, 128GB storage, 48MP camera")
                .price(new BigDecimal("95000.00"))
                .category("Electronics")
                .build());

        Product headphones = productRepository.save(Product.builder()
                .name("Wireless Headphones")
                .description("Active noise cancelling, 30-hour battery life")
                .price(new BigDecimal("12500.00"))
                .category("Electronics")
                .build());

        Product tshirt = productRepository.save(Product.builder()
                .name("Premium Cotton T-Shirt")
                .description("100% organic cotton, breathable fabric")
                .price(new BigDecimal("2500.00"))
                .category("Clothing")
                .build());

        Product backpack = productRepository.save(Product.builder()
                .name("Travel Backpack")
                .description("40L capacity, waterproof, laptop compartment")
                .price(new BigDecimal("7500.00"))
                .category("Accessories")
                .build());

        Product watch = productRepository.save(Product.builder()
                .name("Smart Watch Pro")
                .description("Heart rate monitor, GPS, 5ATM water resistance")
                .price(new BigDecimal("35000.00"))
                .category("Electronics")
                .build());

        Product keyboard = productRepository.save(Product.builder()
                .name("Mechanical Keyboard")
                .description("Cherry MX switches, RGB backlight, wireless")
                .price(new BigDecimal("15000.00"))
                .category("Electronics")
                .build());

        Product mouse = productRepository.save(Product.builder()
                .name("Ergonomic Mouse")
                .description("Wireless, adjustable DPI, comfortable grip")
                .price(new BigDecimal("4500.00"))
                .category("Electronics")
                .build());

        Product charger = productRepository.save(Product.builder()
                .name("65W USB-C Charger")
                .description("GaN technology, compact design, dual ports")
                .price(new BigDecimal("6000.00"))
                .category("Accessories")
                .build());

        Product case_ = productRepository.save(Product.builder()
                .name("Laptop Sleeve Case")
                .description("Shock-proof, fits up to 15.6 inch laptops")
                .price(new BigDecimal("3000.00"))
                .category("Accessories")
                .build());

        List<Product> allProducts = List.of(laptop, phone, headphones, tshirt, backpack,
                watch, keyboard, mouse, charger, case_);

        log.info("Created {} products", allProducts.size());

        int[] colomboStock = {20, 30, 50, 100, 40, 15, 25, 60, 45, 35};
        int[] kandyStock = {10, 15, 30, 60, 20, 8, 12, 35, 25, 20};
        int[] galleStock = {5, 8, 20, 40, 10, 3, 6, 20, 15, 10};

        for (int i = 0; i < allProducts.size(); i++) {
            inventoryRepository.save(BranchInventory.builder()
                    .branchId(colombo.getId())
                    .productId(allProducts.get(i).getId())
                    .stockQuantity(colomboStock[i])
                    .build());
            inventoryRepository.save(BranchInventory.builder()
                    .branchId(kandy.getId())
                    .productId(allProducts.get(i).getId())
                    .stockQuantity(kandyStock[i])
                    .build());
            inventoryRepository.save(BranchInventory.builder()
                    .branchId(galle.getId())
                    .productId(allProducts.get(i).getId())
                    .stockQuantity(galleStock[i])
                    .build());
        }

        log.info("Seeded inventory for all branches");
        log.info("Database seeding complete!");
    }
}
