package com.adorsys.webank.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EntityIndexesTest {

    @Test
    void testPersonalInfoEntityIndexes() {
        Table table = PersonalInfoEntity.class.getAnnotation(Table.class);
        assertNotNull(table, "Table annotation should be present");
        
        Index[] indexes = table.indexes();
        assertEquals(4, indexes.length, "Should have 4 indexes");
        
        assertTrue(containsIndex(indexes, "idx_account_id", "account_id"), "Should have account_id index");
        assertTrue(containsIndex(indexes, "idx_document_id", "document_id"), "Should have document_id index");
        assertTrue(containsIndex(indexes, "idx_status", "status"), "Should have status index");
        assertTrue(containsIndex(indexes, "idx_email", "email"), "Should have email index");
    }

    @Test
    void testOtpEntityIndexes() {
        Table table = OtpEntity.class.getAnnotation(Table.class);
        assertNotNull(table, "Table annotation should be present");
        
        Index[] indexes = table.indexes();
        assertEquals(4, indexes.length, "Should have 4 indexes");
        
        assertTrue(containsIndex(indexes, "idx_public_key_hash", "publicKeyHash"), "Should have publicKeyHash index");
        assertTrue(containsIndex(indexes, "idx_phone_number", "phone_number"), "Should have phone_number index");
        assertTrue(containsIndex(indexes, "idx_status", "status"), "Should have status index");
        assertTrue(containsIndex(indexes, "idx_created_at", "created_at"), "Should have created_at index");
    }

    @Test
    void testUserDocumentsEntityIndexes() {
        Table table = UserDocumentsEntity.class.getAnnotation(Table.class);
        assertNotNull(table, "Table annotation should be present");
        
        Index[] indexes = table.indexes();
        assertEquals(2, indexes.length, "Should have 2 indexes");
        
        assertTrue(containsIndex(indexes, "idx_account_id", "account_id"), "Should have account_id index");
        assertTrue(containsIndex(indexes, "idx_status", "status"), "Should have status index");
    }

    @Test
    void testAllEntitiesHaveTableAnnotation() {
        Reflections reflections = new Reflections("com.adorsys.webank.domain", Scanners.TypesAnnotated);
        Set<Class<?>> entities = reflections.getTypesAnnotatedWith(Entity.class);
        
        for (Class<?> entity : entities) {
            assertTrue(entity.isAnnotationPresent(Table.class), 
                "Entity " + entity.getSimpleName() + " should have @Table annotation");
        }
    }

    private boolean containsIndex(Index[] indexes, String name, String columnList) {
        for (Index index : indexes) {
            if (index.name().equals(name) && index.columnList().equals(columnList)) {
                return true;
            }
        }
        return false;
    }
} 