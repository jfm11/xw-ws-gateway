package com.yada.model

/**
 * 操作
 */
enum class Operator(val op: String) {
    READ("READ"),
    CREATE("CREATE"),
    UPDATE("UPDATE"),
    DELETE("DELETE")
}

/**
 * 机构
 * @param id 机构ID
 * @param name 机构名称
 */
data class Org(
        val id: String,
        val name: String
)

/**
 * 用户
 * @param id 用户ID
 * @param orgId 用户所属机构
 * @param roles 用户拥有角色列表
 */
data class User(
        val id: String,
        val orgId: String,
        val roles: Set<String>
)

/**
 * 资源
 * @param uri 资源的地址
 * @param ops 资源允许的操作
 */
data class Res(val uri: String, val ops: Set<Operator>)

/**
 * 服务
 * @param id 服务ID
 * @param resources 服务对应的资源列表
 */
data class Svc(val id: String, val resources: Set<Res>)

/**
 * 角色
 * @param id 角色ID
 * @param svcs 角色拥有的服务列表
 */
data class Role(
        val id: String,
        val svcs: Set<Svc>
)

