package com.yada.services

import com.yada.model.Org
import com.yada.repository.OrgRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * 机构树
 * @param org 当前机构
 * @param children 子机构
 */
data class OrgTree(val org: Org, val children: Set<OrgTree>?)

/**
 * 机构接口
 */
interface IOrgService {
    /**
     * 获取机构树
     * @param orgIdPrefix 机构ID的前缀
     */
    fun getTree(orgIdPrefix: String?): Flux<OrgTree>

    /**
     * 创建或更新机构
     * @param org 机构
     */
    fun createOrUpdate(org: Org): Mono<Org>

    /**
     * 删除机构
     * @param id 机构号
     */
    fun delete(id: String): Mono<Void>

    /**
     * 获取机构
     * @param id 机构号
     */
    fun get(id: String): Mono<Org>

    /**
     * 根据ID判断机构是否存在
     * @param id 机构号
     */
    fun exist(id: String): Mono<Boolean>
}

fun Org.isMyOffspring(org: Org) = org.id.startsWith(this.id)

/**
 * 生成机构树
 * @param orgs 机构列表
 */
private fun makeTree(orgs: List<Org>): List<OrgTree> {
    val ret = ArrayList<OrgTree>()
    var tmp = orgs
    while (tmp.isNotEmpty()) {
        val o = tmp.first()
        val childrenOrgTree = tmp.drop(1)
                .partition(o::isMyOffspring)
                .run {
                    tmp = second
                    if (first.isEmpty()) null else makeTree(first).toSet()
                }

        ret.add(OrgTree(o, childrenOrgTree))
    }
    return ret
}

@Service
open class OrgService @Autowired constructor(private val repo: OrgRepository,
                                             private val userSvc: IUserService) : IOrgService {
    override fun getTree(orgIdPrefix: String?): Flux<OrgTree> =
            repo.findByIdStartingWithOrderByIdAsc(orgIdPrefix ?: "")//("^${orgIdPrefix ?: ""}.*")
                    .collectList()
                    .map(::makeTree)
                    .flatMapMany { Flux.fromIterable(it) }

    @Transactional
    override fun createOrUpdate(org: Org): Mono<Org> = repo.save(org)

    @Transactional
    override fun delete(id: String): Mono<Void> = userSvc.deleteByOrgId(id).then(repo.deleteById(id))

    override fun get(id: String): Mono<Org> = repo.findById(id)

    override fun exist(id: String): Mono<Boolean> = repo.existsById(id)

}
