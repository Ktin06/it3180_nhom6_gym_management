package vn.edu.hust.nhom6.gym.feature.packagecatalog.dto;

import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Setter được gọi cả với null; tập provided phân biệt trường thiếu với null rõ ràng. */
public class UpdatePackageRequest {
    private final Set<String> provided = new HashSet<>();
    @Getter @JsonDeserialize(using = PackageJsonValues.StringValue.class)
    private String name;
    @Getter @JsonDeserialize(using = PackageJsonValues.IntegerValue.class)
    private Integer durationDays;
    @Getter @JsonDeserialize(using = PackageJsonValues.IntegerValue.class)
    private Integer sessionCount;
    @Getter @JsonDeserialize(using = PackageJsonValues.IntegerValue.class)
    private Integer price;
    @Getter @JsonDeserialize(using = PackageJsonValues.StringValue.class)
    private String description;

    @JsonSetter("name")
    public void setName(String value) { provided.add("name"); name = value; }
    @JsonSetter("durationDays")
    public void setDurationDays(Integer value) { provided.add("durationDays"); durationDays = value; }
    @JsonSetter("sessionCount")
    public void setSessionCount(Integer value) { provided.add("sessionCount"); sessionCount = value; }
    @JsonSetter("price")
    public void setPrice(Integer value) { provided.add("price"); price = value; }
    @JsonSetter("description")
    public void setDescription(String value) { provided.add("description"); description = value; }

    public boolean has(String field) { return provided.contains(field); }

    @JsonAnySetter
    public void rejectUnknownField(String field, Object value) {
        throw new IllegalArgumentException("Request chứa trường không được phép.");
    }
}
