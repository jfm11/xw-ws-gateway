package com.yada

import io.jsonwebtoken.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.util.pattern.PathPatternParser
import java.time.Duration
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * 网上抄的一个kotlin通用logger实现
 * 用的是第三种方法，属性委托
 * 网址：https://www.reddit.com/r/Kotlin/comments/8gbiul/slf4j_loggers_in_3_ways/
 */
class LoggerDelegate : ReadOnlyProperty<Any?, Logger> {
    /**
     * 匿名的伴生对象
     */
    companion object {
        private fun <T> createLogger(clazz: Class<T>): Logger {
            return LoggerFactory.getLogger(clazz)
        }
    }

    private var logger: Logger? = null

    /**
     * 重写取值方法。
     * [thisRef]为被委托属性的所在对象引用，[property]为被委托属性的元数据
     */
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): Logger {
        if (logger == null) {
            logger = createLogger(thisRef!!.javaClass)
        }
        return logger!!
    }
}

/**
 * 参考：https://dzone.com/articles/spring-boot-security-json-web-tokenjwt-hello-world
 */
private const val TOKEN_EXPIRE_INTERVAL: Long = 1 * 60 * 60 // 单位：秒

/**
 * 日期工具
 */
@Component
class TimeUtil {
    fun getCurrentDate() = Date()
}

/**
 * Jwt生成工具
 * @param secret 生成jwt的密钥
 * @param timeUtil 日期工具
 */
@Component
class JwtTokenUtil @Autowired constructor(@Value("\${jwt.secret:yadajwt}") private val secret: String, private val timeUtil: TimeUtil) {
    /**
     * 获取超时日期
     */
    private fun Date.getExpirationDate() = Date(this.time + TOKEN_EXPIRE_INTERVAL * 1000)

    /**
     * 基于HS512签名算法生成token
     */
    private fun JwtBuilder.generateToken() = this.signWith(SignatureAlgorithm.HS512, secret).compact()

    /**
     * 清理cookie中的token
     */
    private val emptyCookie = ResponseCookie.from("token", "").path(getPath(false)).maxAge(0).build()

    /**
     * 清理管理员用户cookie中的token
     */
    private val adminEmptyCookie = ResponseCookie.from("token", "").path(getPath(true)).maxAge(0).build()
    // TODO 管理员用户和普通用户的path为什么是不一样的，有什么用途
    /**
     * 获取空的cookie
     */
    fun getEmptyCookie(entity: AuthInfo): ResponseCookie = if (entity.isAdmin) adminEmptyCookie else emptyCookie

    /**
     * 根据token获取用户认证信息
     */
    fun getEntity(token: String) =
            try {
                AuthInfo(Jwts.parser().setSigningKey(secret).parseClaimsJws(token).body)
            } catch (_: SignatureException) {
                null
            } catch (_: ExpiredJwtException) {
                null
            }

    /**
     * 根据认证信息生成一段时间有效的token
     * @param entity 认证信息
     * @param currentDate 当前日期
     */
    fun generateToken(entity: AuthInfo, currentDate: Date = timeUtil.getCurrentDate()): String =
            Jwts.builder().setClaims(entity).setIssuedAt(currentDate).setExpiration(currentDate.getExpirationDate()).generateToken()

    /**
     * 根据认证信息生成一段时间有效的cookie
     * @param entity 认证信息
     * @param currentDate 当前日期
     */
    fun generateCookie(entity: AuthInfo, currentDate: Date = timeUtil.getCurrentDate()): ResponseCookie {
        val token = generateToken(entity, currentDate)
        return generateCookie(token, Duration.ofMillis(entity.expiration.time - currentDate.time), getPath(entity.isAdmin))
    }

    /**
     * 重新生成cookie
     * @param entity 认证信息
     * @param currentDate 当前日期
     */
    fun renewCookie(entity: AuthInfo, currentDate: Date = timeUtil.getCurrentDate()): ResponseCookie = generateCookie(
            Jwts.builder().setClaims(entity).setIssuedAt(currentDate).setExpiration(currentDate.getExpirationDate()).generateToken(),
            Duration.ofMillis(TOKEN_EXPIRE_INTERVAL * 1000),
            getPath(entity.isAdmin))

    /**
     * 生成浏览器的cookie
     * @param token jwt
     * @param maxAge 最大有效时间
     * @param path cookie的path属性
     */
    private fun generateCookie(token: String, maxAge: Duration, path: String) = ResponseCookie.from("token", token)
            .maxAge(maxAge)
            .path(path)
            .build()

    /**
     * 根据权限获取路径
     * @param isAdmin 是否是管理员用户
     */
    private fun getPath(isAdmin: Boolean?) = if (isAdmin == true) "/admin" else "/"


}

/**
 * 扩展 ServerRequest 对象属性 authInfo，值存储在attributes中，key为authInfo。
 * @see ServerRequest
 */
var ServerRequest.authInfo: AuthInfo
    get() = this.attributes()["authInfo"]!! as AuthInfo
    set(value) {
        this.attributes()["authInfo"] = value
    }

/**
 * 扩展 ServerRequest 对象只读属性 token，存储在cookie中，key为token
 */
val ServerRequest.token: String?
    get() = this.cookies()["token"]?.run { this[0]?.value }

/**
 * 扩展 ServerWebExchange 对象只读属性 token，存储在request属性对cookie中，key为token
 */
val ServerWebExchange.token: String?
    get() = this.request.cookies["token"]?.run { this[0]?.value }

/**
 * 路径解析工具实例
 */
val pathPatternParser = PathPatternParser()