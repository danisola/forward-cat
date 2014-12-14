package com.forwardcat.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

class SerializationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public <T> void assertSerializes(T object, Class<T> clazz) {
        try {
            String serialized = MAPPER.writeValueAsString(object);
            T deserialized = MAPPER.readValue(serialized, clazz);
            assertNotNull(deserialized);
            String serialized2 = MAPPER.writeValueAsString(deserialized);
            assertThat(serialized, is(serialized2));
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }
}
