package com.yada.services

import com.yada.JwtTokenUtil
import com.yada.model.Operator
import com.yada.model.Res
import com.yada.model.User
import com.yada.pathPatternParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.server.PathContainer
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/***
 * 授权服务接口
 */
interface IAuthorizationService {
    /**
     * 授权
     * @param token jwt
     * @param uri 资源地址
     * @param opt 操作
     * @return true-允许；false-不允许
     */
    fun authorize(token: String, uri: String, opt: Operator): Mono<Boolean>

    /**
     * 获取用户资源列表
     * @param user 用户
     * @return 用户拥有的资源列表
     */
    fun getUserResList(user: User): Mono<List<Res>>

    /**
     * 过滤出支持的api
     * @param apiList api列表
     * @param userResList 用户资源列表
     * @return 支持的api
     */
    fun filterApis(apiList: List<Res>, userResList: List<Res>): Mono<List<Res>>
}

/**
 * 拼接服务和uri
 */
private fun svcUri(svcId: String, uri: String) = "/${svcId}${uri}"

@Service
class AuthorizationService @Autowired constructor(
        private val roleService: IRoleService,
        private val jwtUtil: JwtTokenUtil) : IAuthorizationService {

    override fun authorize(token: String, uri: String, opt: Operator): Mono<Boolean> =
            Mono.just(jwtUtil.getEntity(token)!!.resList!!.any { it.uri == uri && opt in it.ops })

    override fun getUserResList(user: User): Mono<List<Res>> =
            // 获取全部的角色
            roleService.getAll()
                    // 找到用户拥有的角色
                    .filter { it.id in user.roles }
                    // 找到资源
                    .map { role ->
                        role.svcs.flatMap { svc ->
                            svc.resources.map { Res(svcUri(svc.id, it.uri), it.ops) }
                        }
                    }
                    // 合并资源
                    .reduce { s, e -> s + e }
                    .map(this::mergeRes)

    override fun filterApis(apiList: List<Res>, userResList: List<Res>): Mono<List<Res>> {
        val resParsers = userResList
                .map { res ->
                    object {
                        val parser = pathPatternParser.parse(res.uri)
                        val ops = res.ops
                    }
                }
        val retList = apiList.mapNotNull { apiRes ->
            resParsers
                    .firstOrNull {
                        // 找到第一个匹配的用户资源
                        it.parser.matches(PathContainer.parsePath(apiRes.uri))
                    }
                    ?.run {
                        // 找到API资源里存在的操作
                        apiRes.copy(ops = apiRes.ops.filter { it in ops }.toSet())
                    }
        }

        return Mono.just(retList)
    }

    // 合并相同uri的ops
    private fun mergeRes(resList: List<Res>) = resList.groupBy { it.uri }.map { entry -> Res(entry.key, entry.value.flatMap { it.ops }.toSet()) }

}