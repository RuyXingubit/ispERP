package br.dev.xb.isperp.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Builder
public class Problem {

    private final Integer status;
    private final String type;
    private final String title;
    private final String detail;
    private final String userMessage;
    private final OffsetDateTime timestamp;
    
    @Nullable
    private final List<Field> objects;

    @Getter
    @Builder
    public static class Field {
        private final String name;
        private final String userMessage;
    }
}
