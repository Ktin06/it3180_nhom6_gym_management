package vn.edu.hust.nhom6.gym.feature.groupclass.dto;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/** ID và số lượng phải là số nguyên JSON; không cắt phần thập phân hoặc ép chuỗi. */
public final class ClassJsonValues {
    private ClassJsonValues() {}
    public static class LongValue extends ValueDeserializer<Long> {
        @Override public Long deserialize(JsonParser p, DeserializationContext c) {
            if (!p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return c.reportInputMismatch(Long.class, "Giá trị phải là số nguyên.");
            }
            return p.getLongValue();
        }
    }
    public static class IntegerValue extends ValueDeserializer<Integer> {
        @Override public Integer deserialize(JsonParser p, DeserializationContext c) {
            if (!p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return c.reportInputMismatch(Integer.class, "Giá trị phải là số nguyên.");
            }
            return p.getIntValue();
        }
    }
}
