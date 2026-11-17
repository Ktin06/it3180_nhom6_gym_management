package vn.edu.hust.nhom6.gym.feature.packagecatalog.dto;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/** Không ép chuỗi/số thực thành số nguyên hoặc ép scalar thành chuỗi trong request F3. */
public final class PackageJsonValues {
    private PackageJsonValues() {}

    public static class TypeValue extends ValueDeserializer<PackageType> {
        @Override
        public PackageType deserialize(JsonParser parser, DeserializationContext context) {
            if (parser.hasToken(JsonToken.VALUE_STRING)) {
                try { return PackageType.valueOf(parser.getString()); }
                catch (IllegalArgumentException ignored) {
                    return context.reportInputMismatch(PackageType.class, "Loại gói phải là TIME, SESSION hoặc PT.");
                }
            }
            return context.reportInputMismatch(PackageType.class, "Loại gói phải là TIME, SESSION hoặc PT.");
        }
    }

    public static class IntegerValue extends ValueDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext context) {
            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return context.reportInputMismatch(Integer.class, "Giá trị phải là số nguyên.");
            }
            return parser.getIntValue();
        }
    }

    public static class StringValue extends ValueDeserializer<String> {
        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) {
            if (!parser.hasToken(JsonToken.VALUE_STRING)) {
                return context.reportInputMismatch(String.class, "Giá trị phải là chuỗi.");
            }
            return parser.getString();
        }
    }
}
