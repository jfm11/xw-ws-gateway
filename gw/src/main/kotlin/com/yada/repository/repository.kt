package com.yada.repository

import com.yada.model.Org
import com.yada.model.Role
import com.yada.model.Svc
import com.yada.model.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * 顶级用户存储
 */
interface IUserRepository {
    /**
     * 修改密码
     * @param id 用户ID
     * @param pwd 密码
     */
    fun changePwd(id: String, pwd: String): Mono<Void>
}

/**
 * 顶级用户存储实现
 */
class UserRepositoryImpl @Autowired constructor(private val reactiveMongoTemplate: ReactiveMongoTemplate) : IUserRepository {
    override fun changePwd(id: String, pwd: String): Mono<Void> {
        val query = org.springframework.data.mongodb.core.query.Query(Criteria.where("id").`is`(id))
        val update = Update().set("pwd", pwd)
        return reactiveMongoTemplate.updateFirst(query, update, User::class.java).then(Mono.create<Void> { it.success() })
    }
}

/**
 * 机构存储
 */
interface OrgRepository : ReactiveCrudRepository<Org, String> {
    /**
     * 通过机构ID查询以该ID开头的机构
     * @param regex 机构ID
     * @return 带排序的机构实体
     */
    fun findByIdStartingWithOrderByIdAsc(regex: String): Flux<Org>
}

/**
 * 用户存储
 */
interface UserRepository : IUserRepository, ReactiveCrudRepository<User, String> {
    /**
     * 根据机构ID查询用户
     * @param orgId 机构ID
     * @return 带排序的用户实体
     */
    fun findByOrgIdOrderByIdAsc(orgId: String): Flux<User>

    /**
     * 查询用户密码
     * @param id 用户ID
     * @return 密码
     */
    @Query("{'id': ?0}", fields = "{'pwd': 1, '_id': 0}")
    fun fundOnPwd(id: String): Mono<String>

    /**
     * 通过机构ID删除用户
     * @param orgId 机构ID
     */
    fun deleteByOrgId(orgId: String): Mono<Void>
}

/**
 * 服务存储
 */
interface SvcRepository : ReactiveCrudRepository<Svc, String> {
    /**
     * 查询所有服务
     * @return 带排序的服务
     */
    fun findAllByOrderByIdAsc(): Flux<Svc>
}

/**
 * 角色存储
 */
interface RoleRepository : ReactiveCrudRepository<Role, String> {
    /**
     * 查询所有角色
     * @return 带排序的角色
     */
    fun findAllByOrderByIdAsc(): Flux<Role>
}

/**
 * 管理员用户
 * @param id 用户ID
 * @param pwd 密码
 */
data class AdminUser(val id: String, val pwd: String)

/**
 * 管理员用户存储
 */
interface IAdminUserRepository {
    /**
     * 修改密码
     * @param pwd 新密码
     */
    fun changePwd(pwd: String): Mono<Void>

    /**
     * 获取管理员用户
     * @return 管理员用户
     */
    fun getAdminUser(): Mono<AdminUser>
}

/**
 * 管理员用户存储实现
 */
@Component
class AdminUserRepositoryImpl @Autowired constructor(private val reactiveMongoTemplate: ReactiveMongoTemplate) : IAdminUserRepository {
    private val collectionName = "admin"

    override fun changePwd(pwd: String): Mono<Void> {
        return reactiveMongoTemplate.save(AdminUser("admin", pwd), collectionName).then()
    }

    override fun getAdminUser(): Mono<AdminUser> {
        val query = org.springframework.data.mongodb.core.query.Query(Criteria.where("id").`is`("admin"))
        return reactiveMongoTemplate.findOne(query, collectionName)
    }

}
