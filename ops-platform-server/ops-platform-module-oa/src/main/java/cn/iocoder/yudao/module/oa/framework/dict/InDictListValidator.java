package cn.iocoder.yudao.module.oa.framework.dict;

import cn.iocoder.yudao.module.oa.service.dict.DictService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InDictListValidator implements ConstraintValidator<InDictList, List<String>> {

    private final DictService dictService;
    private String dictType;

    @Override
    public void initialize(InDictList annotation) {
        this.dictType = annotation.value();
    }

    @Override
    public boolean isValid(List<String> values, ConstraintValidatorContext context) {
        if (values == null || values.isEmpty()) {
            return true;
        }
        for (String value : values) {
            if (value == null || !dictService.isValidValue(dictType, value)) {
                return false;
            }
        }
        return true;
    }
}
