package io.pivotal.spinnaker

data class Pipeline(val stages: List<Stage>)

data class Stage(val id: String,
                 val type: String,
                 val context: Map<String, Any>,
                 val status: String,
                 val tasks: List<Task>)

data class Task(val status: String,
                val name: String)

data class Context(val teamName: String,
                   val pipelineName: String,
                   val resourceName: String,
                   val job: String?,
                   val buildNumber: Int?)