package io.pivotal.spinnaker

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import java.io.File
import java.time.Duration

object SpinnakerConcourseResource {
    private val mapper: ObjectMapper = ObjectMapper()
            .registerModule(KotlinModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    @JvmStatic
    fun main(args: Array<String>) {
        when (args.firstOrNull()) {
            "check" -> {
                println(mapper.writeValueAsString(check()))
            }
            "in" -> {
                val destination = File(args[1])
                destination.mkdirs()
                println(mapper.writeValueAsString(doIn(destination)))
            }
            "out" -> {
                println("I did some output")
            }
            else -> throw IllegalArgumentException("Requires 'in', 'check', or 'out' parameter")
        }
    }

    private fun request() = mapper.readValue<Request>(readLine()!!)

    fun doIn(destination: File, request: Request = request()): InResponse {
        val source = request.source
        val stageId = request.version!!.ref

        val stage = executingPipelines(source)
                .map { pipeline -> pipeline.stages.first { stage -> stage.id == stageId && stage.status == "RUNNING" } }
                .firstOrNull() ?: throw IllegalStateException("A stage with a matching id was not found or it was no longer in the RUNNING state")

        webClient(source)
                .post()
                .uri { builder ->
                    val context = mapper.convertValue<Context>(stage.context)
                    builder.path("/concourse/stage/start")
                            .queryParam("stageId", stageId)
                            .queryParam("jobName", context.job!!)
                            .queryParam("buildNumber", context.buildNumber!!)
                            .build()
                }
                .exchange()
                .block(Duration.ofSeconds(30))

        return InResponse(request.version)
    }

    fun check(request: Request = request()): List<Version> {
        val source = request.source
        return executingPipelines(source)
                .map { pipeline ->
                    pipeline.stages.filter { stage ->
                        stage.type == "concourse" &&
                                stage.status == "RUNNING" &&
                                mapper.convertValue<Context>(stage.context).run {
                                    teamName == source.teamName &&
                                            pipelineName == source.pipelineName &&
                                            resourceName == source.resourceName
                                } &&
                                stage.tasks.any { task ->
                                    task.name == "waitForConcourseJobStartTask" &&
                                            task.status == "RUNNING"
                                }
                    }
                }
                .flatten()
                .map { stage -> Version(stage.id) }
    }

    private fun executingPipelines(source: Source) = webClient(source).get()
            .uri { builder ->
                builder
                        .path("applications/${source.spinnakerApp}/executions/search")
                        .queryParam("statuses", "RUNNING")
                        .queryParam("expand", "true")
                        .build()
            }
            .retrieve()
            .bodyToFlux(Pipeline::class.java)
            .toIterable()

    private fun webClient(source: Source) = WebClient.builder()
            .exchangeStrategies(ExchangeStrategies.builder().codecs { config ->
                config.defaultCodecs()
                        .jackson2JsonDecoder(Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON))
            }.build())
            .baseUrl(source.spinnakerGate)
            .build()
}

data class Request(val source: Source,
                   val version: Version?)

data class InResponse(val version: Version)

data class Source(val spinnakerGate: String,
                  val spinnakerApp: String,
                  val pipelineName: String,
                  val resourceName: String,
                  val teamName: String = "main")

data class Version(val ref: String)