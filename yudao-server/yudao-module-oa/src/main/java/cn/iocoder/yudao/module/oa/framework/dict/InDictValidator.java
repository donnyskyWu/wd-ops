package cn.iocoder.yudao.module.oa.framework.dict;

import cn.iocoder.yudao.module.oa.service.dict.DictService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InDictValidator implements ConstraintValidator<InDict, String> {

    private final DictService dictService;
    private String dictType;

    @Override
    public void initialize(InDict annotation) {
        this.dictType = annotation.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return dictService.isValidValue(dictType, value);
    }
}
