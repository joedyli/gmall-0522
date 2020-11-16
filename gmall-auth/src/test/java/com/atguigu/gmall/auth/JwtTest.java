package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
	private static final String pubKeyPath = "D:\\project-0522\\rsa\\rsa.pub";
    private static final String priKeyPath = "D:\\project-0522\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 2);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2MDU1MTI2ODN9.N6ZhhIJZ6qssusLgMy90fQwGQkWl9fhKI4znl-BQw2VuArwtkzt4VPn3MkEbGw4s5mg-bqfHEWCxoHyrBE1CcofqUnrUd4Z6TLGfL1gtzA-z4ESumDHGhtOH2v1_wYULW2-9nlAgIaBn6PwTkmR2Dtrqi8VBLSng41-0iBrBZzhiXIBClRFqR2FvZ4Jg9e93SrZgF4NWDV3TneRAnQQtpkEwDjrq6F_81N4NN4lUeX7S-U9pc1AblfxF5jRzWhntvFS-NuoqznJ8CE5BRgVcGY8AFZZ5qpzjIwAUF-lb6niCEVBaPARavd3aNOw4TQ1urIQMxslUyfJFCPv4AYtiXg";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}
