package com.yada.services

import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 密码接口
 */
interface IPwdDigestService {
    /**
     * 获取默认的密码摘要
     * @param username 用户名
     */
    fun getDefaultPwdDigest(username: String): String

    /**
     * 根据密码获取密码摘要
     * @param username 用户名
     * @param pwdPlaintext 密码明文
     */
    fun getPwdDigest(username: String, pwdPlaintext: String): String
}

// TODO 密码安全性不符合要求
@Service
class PwdDigestService @Autowired constructor(@Value("\${yada.user.defaultPwd:changepwd}") private val defaultPwd: String) : IPwdDigestService {
    override fun getDefaultPwdDigest(username: String): String = getPwdDigest(username, defaultPwd)

    override fun getPwdDigest(username: String, pwdPlaintext: String): String = Base64.encodeBase64String(DigestUtils.sha1(username + pwdPlaintext))
}