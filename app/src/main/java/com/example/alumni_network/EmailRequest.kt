package com.example.alumni_network

data class EmailRequest(
    val personalizations: List<Personalization>,
    val from: From,
    val subject: String,
    val content: List<Content>
)
data class Personalization(
    val to: List<To>
)

data class To(
    val email: String
)

data class From(
    val email: String
)

data class Content(
    val type: String,
    val value: String
)
