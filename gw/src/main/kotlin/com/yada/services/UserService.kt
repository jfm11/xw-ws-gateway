package com.yada.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.yada.model.User
import com.yada.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * 用户服务接口
 */
interface IUserService {
    /**
     * 通过用户ID获取用户
     */
    fun get(id: String): Mono<User>

    /**
     * 通过机构ID获取用户
     */
    fun getByOrgId(orgId: String): Flux<User>

    /**
     * 创建或更新用户
     */
    fun createOrUpdate(user: User): Mono<User>

    /**
     * 通过用户ID删除用户
     */
    fun delete(id: String): Mono<Void>

    /**
     * 通过机构ID删除用户
     */
    fun deleteByOrgId(orgId: String): Mono<Void>

    /**
     * 通过ID检查用户是否存在
     */
    fun exist(id: String): Mono<Boolean>

    /**
     * 获取通过用户ID获取密码
     */
    fun getPwd(id: String): Mono<String>

    /**
     * 通过ID修改用户密码
     */
    fun changePwd(id: String, pwd: String): Mono<Void>

    /**
     * 获取所有用户
     */
    fun getAll(): Flux<User>
}

/**
 * 用户服务实现类
 */
@Service
open class UserService @Autowired constructor(private val userRepo: UserRepository,
                                              private val pwdDigestService: IPwdDigestService) : IUserService {
    override fun get(id: String): Mono<User> = userRepo.findById(id)

    override fun getByOrgId(orgId: String): Flux<User> = userRepo.findByOrgIdOrderByIdAsc(orgId)

    @Transactional
    override fun createOrUpdate(user: User): Mono<User> =
            userRepo.findById(user.id)
                    // 查到用户，执行更新
                    .flatMap { userRepo.save(user) }
                    // 未查到用户，执行保存
                    .switchIfEmpty(
                            userRepo.save(user)
                                    .flatMap {
                                        // 设置默认密码
                                        userRepo.changePwd(it.id, pwdDigestService.getDefaultPwdDigest(it.id))
                                                .then(Mono.just(it))
                                    })

    @Transactional
    override fun delete(id: String): Mono<Void> = userRepo.deleteById(id)

    @Transactional
    override fun deleteByOrgId(orgId: String): Mono<Void> = userRepo.deleteByOrgId(orgId)

    override fun exist(id: String): Mono<Boolean> = userRepo.existsById(id)

    override fun getPwd(id: String): Mono<String> =
            userRepo.fundOnPwd(id)
                    .map { ObjectMapper().readTree(it)["pwd"]?.asText() }

    @Transactional
    override fun changePwd(id: String, pwd: String): Mono<Void> = userRepo.changePwd(id, pwd)

    override fun getAll(): Flux<User> = userRepo.findAll()
}
