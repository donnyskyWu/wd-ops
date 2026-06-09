package cn.iocoder.yudao.module.oa.util;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class AesUtil {

    private final AES aes;

    public AesUtil(@Value("${oa.crypto.aes-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        this.aes = SecureUtil.aes(keyBytes);
    }

    public String encrypt(String plainText) {
        try {
            return aes.encryptBase64(plainText, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "加密失败");
        }
    }

    public String decrypt(String cipherText) {
        try {
            return aes.decryptStr(cipherText, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "解密失败");
        }
    }
}
