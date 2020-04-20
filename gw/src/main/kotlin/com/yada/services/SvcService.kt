package com.yada.services

import com.yada.model.Svc
import com.yada.repository.SvcRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * 服务接口
 */
interface ISvcService {
    /**
     * 获取所有服务
     */
    fun getAll(): Flux<Svc>

    /**
     * 根据ID获取服务
     * @param id 服务ID
     * @return 服务实体
     */
    fun get(id: String): Mono<Svc>

    /**
     * 创建或更新服务
     * @param svc 服务实体
     * @return 服务实体
     */
    fun createOrUpdate(svc: Svc): Mono<Svc>

    /**
     * 修改服务ID
     * @param oldId 旧服务ID
     * @param newId 新服务ID
     * @return 新服务实体
     */
    fun changeId(oldId: String, newId: String): Mono<Svc>

    /**
     * 删除服务
     * @param id 服务ID
     */
    fun delete(id: String): Mono<Void>
}

@Service
open class SvcService @Autowired constructor(private val repo: SvcRepository, private val roleSvc: IRoleService) : ISvcService {
    override fun getAll(): Flux<Svc> = repo.findAllByOrderByIdAsc()

    override fun get(id: String): Mono<Svc> = repo.findById(id)

    @Transactional
    override fun createOrUpdate(svc: Svc): Mono<Svc> = repo.save(svc)

    @Transactional
    override fun changeId(oldId: String, newId: String): Mono<Svc> =
            repo.findById(oldId)
                    // 复制原服务信息并新增
                    .flatMap { repo.save(it.copy(id = newId)) }
                    // 删除原服务信息
                    .flatMap { repo.deleteById(oldId).then(Mono.just(it)) }

    @Transactional
    override fun delete(id: String): Mono<Void> = roleSvc.getAll()
            .flatMap { role ->
                // 根据查询到的角色，得到不包含当前ID的服务
                val set = role.svcs.filter { it.id != id }.toSet()
                // 如果新服务数量和旧服务数量不相等，则更新角色对应的资源
                if (set.size != role.svcs.size) roleSvc.createOrUpdate(role.copy(svcs = set)) else Mono.empty()
            }
            // 角色清理完毕后，删除该服务
            .then(repo.deleteById(id))
}
