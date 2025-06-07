package com.c0x12c.featureflag.client

import com.c0x12c.featureflag.jackson.CoreJackson
import com.c0x12c.featureflag.notification.SlackPayload
import com.fasterxml.jackson.databind.ObjectMapper
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface SlackClient {
  @POST(".")
  fun sendMessage(
    @Body payload: SlackPayload,
    @HeaderMap headers: Map<String, String>
  ): retrofit2.Response<Unit>

  companion object {
    fun create(
      webhookUrl: String,
      jackson: ObjectMapper?
    ): SlackClient =
      Retrofit
        .Builder()
        .baseUrl(webhookUrl)
        .addConverterFactory(JacksonConverterFactory.create(jackson ?: CoreJackson.INSTANCE))
        .build()
        .create(SlackClient::class.java)
  }
}
