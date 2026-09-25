package com.allensandiego.adm.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the TestDataSeeder only activates in 'test' and 'dev' profiles.
 */
@SpringBootTest
@ActiveProfiles("test")
class SeedProfileSafetyTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("TestDataSeeder should be active in test profile")
    void seederShouldBeActiveInTestProfile() {
        // The seeder bean is registered as an ApplicationRunner via @Bean method
        String[] beans = applicationContext.getBeanNamesForType(org.springframework.boot.ApplicationRunner.class);
        assertThat(beans).isNotEmpty();
    }
}

@SpringBootTest
@TestPropertySource(properties = "spring.profiles.active=")
class SeedProfileSafetyDefaultTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Seeder should not be active in default profile")
    void seederShouldNotBeActiveInDefaultProfile() {
        // This test runs with no additional profiles (default is just the base app)
        // If we switch to a profile without 'test' or 'dev', the seeder beans should not exist
        assertThat(applicationContext.containsBean("testDataSeeder")).isFalse();
    }
}
