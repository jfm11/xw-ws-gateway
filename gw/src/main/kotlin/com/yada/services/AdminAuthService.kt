package com.yada.services

import com.yada.AuthInfo
import com.yada.JwtTokenUtil
import com.yada.model.Operator
import com.yada.repository.IAdminUserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * 管理员授权接口
 */
interface IAdminAuthService {
    /**
     * 登录
     * @param username 用户名
     * @param password 密码
     * @return 授权信息
     */
    fun login(username: String, password: String): Mono<AuthInfo>

    /**
     * 登出
     * @param token jwt
     */
    fun logout(token: String): Mono<Void>

    /**
     * 修改密码
     * @param username 用户名
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return true-修改成功；false-修改失败
     */
    fun changePassword(username: String, oldPassword: String, newPassword: String): Mono<Boolean>

    /**
     * 授权
     * @param token jwt
     * @param uri 资源URI
     * @param opt 操作
     * @return true-允许；false-拒绝
     */
    fun authorize(token: String, uri: String, opt: Operator): Mono<Boolean>
}

// 管理员常量字符
private const val adminStr = "admin"

/**
 * 管理员授权接口实现
 * @param adminUserRepo 管理员用户存储
 * @param pwdDigestService 密码服务
 * @param jwtUtil jwt工具
 */
@Service
class AdminAuthService @Autowired constructor(private val adminUserRepo: IAdminUserRepository,
                                              private val pwdDigestService: IPwdDigestService,
                                              private val jwtUtil: JwtTokenUtil) : IAdminAuthService {
    override fun login(username: String, password: String): Mono<AuthInfo> = if (username == adminStr) {
        adminUserRepo
                .getAdminUser()
                .map { it.pwd }
                .defaultIfEmpty(pwdDigestService.getDefaultPwdDigest(adminStr))
                .filter { pwdDigestService.getPwdDigest(adminStr, password) == it }
                .map { AuthInfo.create() }
    } else {
        Mono.empty()
    }

    override fun logout(token: String): Mono<Void> = Mono.empty()

    override fun authorize(token: String, uri: String, opt: Operator): Mono<Boolean> = Mono.just(jwtUtil.getEntity(token) != null)

    override fun changePassword(username: String, oldPassword: String, newPassword: String): Mono<Boolean> = if (username == adminStr) {
        val newPwdDigest = pwdDigestService.getPwdDigest(adminStr, newPassword)
        val oldPwdDigest = pwdDigestService.getPwdDigest(adminStr, oldPassword)

        adminUserRepo.getAdminUser()
                .map { it.pwd == oldPwdDigest }
                .defaultIfEmpty(pwdDigestService.getDefaultPwdDigest(adminStr) == oldPwdDigest)
                .filter { it }
                .flatMap { adminUserRepo.changePwd(newPwdDigest).then(Mono.just(true)) }
                .defaultIfEmpty(false)
    } else {
        Mono.just(false)
    }
}