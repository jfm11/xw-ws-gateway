package com.yada.services

import com.yada.AuthInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * 认帐服务接口
 */
interface IAuthenticationService {
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
}

/**
 * 认证接口实现
 * @param userService 用户服务
 * @param pwdDigestService 密码算法服务
 * @param author 授权服务
 */
@Service
class AuthenticationService @Autowired constructor(
        private val userService: IUserService,
        private val pwdDigestService: IPwdDigestService,
        private val author: IAuthorizationService) : IAuthenticationService {
    override fun login(username: String, password: String): Mono<AuthInfo> =
            userService.getPwd(username)
                    .map { it == pwdDigestService.getPwdDigest(username, password) }
                    .filter { it }
                    .flatMap { userService.get(username) }
                    .flatMap { user ->
                        author.getUserResList(user).map { resList -> AuthInfo.create(user, resList) }
                    }

    override fun logout(token: String): Mono<Void> = Mono.empty()

    override fun changePassword(username: String, oldPassword: String, newPassword: String): Mono<Boolean> =
            userService.getPwd(username)
                    .map { it == pwdDigestService.getPwdDigest(username, oldPassword) }
                    .filter { it }
                    .flatMap {
                        userService.changePwd(username, pwdDigestService.getPwdDigest(username, newPassword))
                                .then(Mono.just(true))
                    }
                    .defaultIfEmpty(false)
}