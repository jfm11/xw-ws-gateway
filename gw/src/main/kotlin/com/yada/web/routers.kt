package com.yada.web

import com.yada.web.handlers.AdminAuthHandler
import com.yada.web.handlers.AuthHandler
import com.yada.web.handlers.apis.OrgHandler
import com.yada.web.handlers.apis.RoleHandler
import com.yada.web.handlers.apis.SvcHandler
import com.yada.web.handlers.apis.UserHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

/**
 * 授权路由配置
 */
@Configuration
open class AuthRouterConfig @Autowired constructor(
        private val authHandler: AuthHandler,
        private val authFilter: AuthHandlerFilter,
        private val authApiFilter: AuthApiHandlerFilter,
        private val whitelistFilter: WhitelistHandlerFilter) {
    @Bean
    open fun authRouter() = router {
        "".nest {
            "/login".nest {
                GET("", authHandler::getLoginForm)
                POST("", authHandler::login)
            }
            filter(whitelistFilter)
        }

        "".nest {
            GET("/") { _ ->
                ServerResponse.ok().render("/index")
            }
            filter(whitelistFilter)
            filter { request, next -> authFilter.invoke(request, next) }
        }

        "".nest {
            GET("/logout", authHandler::logout)
            POST("/change_pwd", authHandler::changePwd)
            GET("/refresh_token", authHandler::refreshToken)
            GET("/filter_apis", authHandler::filterApis)
            filter(whitelistFilter)
            filter(authApiFilter)
        }
    }
}

/**
 * 管理员授权路由配置
 */
@Configuration
open class AdminAuthRouterConfig @Autowired constructor(
        private val adminAuthHandler: AdminAuthHandler,
        private val authAdminApiFilter: AuthAdminApiHandlerFilter,
        private val whitelistFilter: WhitelistHandlerFilter) {
    @Bean
    open fun adminAuthRouter() = router {
        "/admin".nest {
            GET("", adminAuthHandler::index)
            POST("/login", adminAuthHandler::login)
            filter(whitelistFilter)
        }

        "/admin".nest {
            POST("/apis/logout", adminAuthHandler::logout)
            POST("/apis/change_pwd", adminAuthHandler::changePwd)
            GET("/apis/refresh_token", adminAuthHandler::refreshToken)
            filter(whitelistFilter)
            filter(authAdminApiFilter)
        }
    }
}

/**
 * 管理员API路由配置
 */
@Configuration
open class AdminApiRouterConfig @Autowired constructor(
        private val roleHandler: RoleHandler,
        private val orgHandler: OrgHandler,
        private val svcHandler: SvcHandler,
        private val userHandler: UserHandler,
        private val authAdminApiFilter: AuthAdminApiHandlerFilter,
        private val whitelistFilter: WhitelistHandlerFilter) {
    @Bean
    open fun adminApiRouter() = router {
        "/admin/apis".nest {
            "/role".nest {
                GET("", roleHandler::getAll)
                GET("/{id}", roleHandler::get)
                GET("/{id}/exist", roleHandler::exist)
                PUT("", roleHandler::createOrUpdate)
                DELETE("/{id}", roleHandler::delete)
            }
            "/org".nest {
                GET("", orgHandler::getTree)
                GET("/{id}", orgHandler::get)
                GET("/{id}/exist", orgHandler::exist)
                PUT("", orgHandler::createOrUpdate)
                DELETE("/{id}", orgHandler::delete)
            }
            "/svc".nest {
                GET("", svcHandler::getAll)
                GET("/{id}", svcHandler::get)
                PUT("", svcHandler::createOrUpdate)
                DELETE("", svcHandler::delete)
            }
            "/user".nest {
                GET("", userHandler::getUsersBy)
                GET("/{id}", userHandler::get)
                GET("/{id}/exist", userHandler::exist)
                PUT("", userHandler::createOrUpdate)
                DELETE("/{id}", userHandler::delete)
            }
        }
        filter(whitelistFilter)
        filter(authAdminApiFilter)
    }
}
// TODO 内部API路由配置