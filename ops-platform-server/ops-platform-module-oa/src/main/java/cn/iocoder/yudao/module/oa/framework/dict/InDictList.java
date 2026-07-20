package cn.iocoder.yudao.module.oa.framework.dict;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = InDictListValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface InDictList {

    String value();

    String message() default "字典值不合法";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
