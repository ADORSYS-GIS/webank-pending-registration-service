package com.adorsys.webank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class EntityConstraintsTest {

    @Test
    void testPersonalInfoEntityConstraints() {
        assertNotNull(getField(PersonalInfoEntity.class, "accountId").getAnnotation(Id.class), 
            "accountId should be annotated with @Id");
        
        Column accountIdColumn = getField(PersonalInfoEntity.class, "accountId").getAnnotation(Column.class);
        assertFalse(accountIdColumn.nullable(), "accountId should not be nullable");
        
        Column documentIdColumn = getField(PersonalInfoEntity.class, "documentUniqueId").getAnnotation(Column.class);
        assertTrue(documentIdColumn.nullable(), "documentUniqueId should be nullable");
        assertEquals(20, documentIdColumn.length(), "documentUniqueId should have length 20");
        
        Column emailColumn = getField(PersonalInfoEntity.class, "email").getAnnotation(Column.class);
        assertEquals(30, emailColumn.length(), "email should have length 30");
    }

    @Test
    void testOtpEntityConstraints() {
        Column phoneNumberColumn = getField(OtpEntity.class, "phoneNumber").getAnnotation(Column.class);
        assertFalse(phoneNumberColumn.nullable(), "phoneNumber should not be nullable");
        assertEquals(20, phoneNumberColumn.length(), "phoneNumber should have length 20");
        
        Column publicKeyHashColumn = getField(OtpEntity.class, "publicKeyHash").getAnnotation(Column.class);
        assertFalse(publicKeyHashColumn.nullable(), "publicKeyHash should not be nullable");
        assertTrue(publicKeyHashColumn.unique(), "publicKeyHash should be unique");
        
        Column otpCodeColumn = getField(OtpEntity.class, "otpCode").getAnnotation(Column.class);
        assertFalse(otpCodeColumn.nullable(), "otpCode should not be nullable");
        assertEquals(20, otpCodeColumn.length(), "otpCode should have length 20");
    }

    @Test
    void testUserDocumentsEntityConstraints() {
        assertNotNull(getField(UserDocumentsEntity.class, "accountId").getAnnotation(Id.class), 
            "accountId should be annotated with @Id");
        
        Column accountIdColumn = getField(UserDocumentsEntity.class, "accountId").getAnnotation(Column.class);
        assertFalse(accountIdColumn.nullable(), "accountId should not be nullable");
        
        Column frontIdColumn = getField(UserDocumentsEntity.class, "frontID").getAnnotation(Column.class);
        assertTrue(frontIdColumn.nullable(), "frontID should be nullable");
        assertEquals("TEXT", frontIdColumn.columnDefinition(), "frontID should be TEXT type");
    }

    private Field getField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            fail("Field " + fieldName + " not found in " + clazz.getSimpleName());
            return null;
        }
    }
} 