package org.oxoho.note.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "jwt")
class JwtProperties(
    var secret: String = "",
    var accessTokenValidityMs: Long = 15 * 60 * 1000L, // 15 minutes
    var refreshTokenValidityMs: Long = 30 * 24 * 60 * 60 * 1000L // 30 days
)
