package cn.iocoder.yudao.module.oa.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AesUtilTest extends cn.iocoder.yudao.module.oa.OaITBase {

    @Autowired
    private AesUtil aesUtil;

    @Test
    void encryptDecryptRoundTrip() {
        String plain = "330101199001011234";
        String cipher = aesUtil.encrypt(plain);
        assertEquals(plain, aesUtil.decrypt(cipher));
    }
}
