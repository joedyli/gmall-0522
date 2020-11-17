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
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2MDU1ODA0MDJ9.PAwU6defnfvWLG--ljNM3Uy7u6cN6gWWLdM3HSzXxewhPfDj5NFu0uZAoI8P_lCdxCNBpYG_cYpU-OSYD1jZkpRVJ9h85ZRrRiM6prJJlicfuT0wKDI25YlV3b-ceVHmlTlxNZZbUfMVZEw6kHY7A4hup1hDshciFz9TcExZLifZCYyDDKhEHrVW6Dqu2ckREE6eKpWPo-t-yN5AKBxtikSElaK0wrAxeFdPjTkvsEXYBdcB2yIdhj_nncQiLQM0WTnDlVxZ9s7GXEEUHPuicncg0cSLCEFsjGkL_uRYHCiK-_bqpA5-xUPYjBLVudexyyhRhCSuydJiuRsKIU443Q";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}
