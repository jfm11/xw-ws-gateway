package com.yada.services

import com.yada.model.Role
import com.yada.repository.RoleRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * 角色接口
 */
interface IRoleService {
    /**
     * 获取所有角色
     */
    fun getAll(): Flux<Role>

    /**
     * 根据ID获取角色
     */
    fun get(id: String): Mono<Role>

    /**
     * 根据ID判断角色是否存在
     */
    fun exist(id: String): Mono<Boolean>

    /**
     * 创建或更新角色
     */
    fun createOrUpdate(role: Role): Mono<Role>

    /**
     * 根据ID删除角色
     */
    fun delete(id: String): Mono<Void>
}

@Service
open class RoleService @Autowired constructor(private val roleRepo: RoleRepository, private val userService: IUserService) : IRoleService {
    override fun getAll(): Flux<Role> = roleRepo.findAllByOrderByIdAsc()

    override fun get(id: String): Mono<Role> = roleRepo.findById(id)

    @Transactional
    override fun exist(id: String): Mono<Boolean> = roleRepo.existsById(id)

    @Transactional
    override fun createOrUpdate(role: Role): Mono<Role> = roleRepo.save(role)

    @Transactional
    override fun delete(id: String): Mono<Void> = userService.getAll()
            // 找到存在当前角色用户
            .filter { user -> id in user.roles }
            // 删除当前用户的该角色
            .map { user -> user.copy(roles = user.roles - id) }
            // 保存或更新用户
            .flatMap(userService::createOrUpdate)
            // 删除角色
            .then(roleRepo.deleteById(id))
}