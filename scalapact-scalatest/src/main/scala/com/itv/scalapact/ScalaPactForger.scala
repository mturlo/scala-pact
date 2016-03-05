package com.itv.scalapact

object ScalaPactForger {

  implicit val options = ScalaPactOptions.DefaultOptions

  object forgePact {
    def between(consumer: String): ScalaPartialPact = new ScalaPartialPact(consumer)

    class ScalaPartialPact(consumer: String) {
      def and(provider: String): ScalaPactDescription = new ScalaPactDescription(consumer, provider, Nil)
    }

    class ScalaPactDescription(consumer: String, provider: String, interactions: List[ScalaPactInteraction]) {

      /**
        * Adds interactions to the Pact. Interactions should be created using the helper object 'interaction'
        * @param interaction [ScalaPactInteraction] definition
        * @return [ScalaPactDescription] to allow the builder to continue
        */
      def addInteraction(interaction: ScalaPactInteraction): ScalaPactDescription = new ScalaPactDescription(consumer, provider, interactions ++ List(interaction))

      def runConsumerTest(test: ScalaPactMockConfig => Unit)(implicit options: ScalaPactOptions): Unit = {
        ScalaPactMock.runConsumerIntegrationTest(
          ScalaPactDescriptionFinal(
            consumer,
            provider,
            interactions.map(i => i.finalise),
            options
          )
        )(test)
      }

    }
  }

  object interaction {
    def description(message: String): ScalaPactInteraction = new ScalaPactInteraction(message, None, ScalaPactRequest.default, ScalaPactResponse.default)
  }

  class ScalaPactInteraction(description: String, providerState: Option[String], request: ScalaPactRequest, response: ScalaPactResponse) {
    def given(state: String): ScalaPactInteraction = new ScalaPactInteraction(description, Option(state), request, response)


    def uponReceiving(path: String): ScalaPactInteraction = uponReceiving(GET, path, None, Map.empty, None)
    def uponReceiving(method: ScalaPactMethod, path: String): ScalaPactInteraction = uponReceiving(method, path, None, Map.empty, None)
    def uponReceiving(method: ScalaPactMethod, path: String, query: Option[String]): ScalaPactInteraction = uponReceiving(method, path, query, Map.empty, None)
    def uponReceiving(method: ScalaPactMethod, path: String, query: Option[String], headers: Map[String, String], body: Option[String]): ScalaPactInteraction = new ScalaPactInteraction(
      description,
      providerState,
      ScalaPactRequest(method, path, query, headers, body),
      response
    )

    def willRespondWith(status: Int): ScalaPactInteraction = willRespondWith(status, Map.empty, None)
    def willRespondWith(status: Int, body: String): ScalaPactInteraction = willRespondWith(status, Map.empty, Option(body))
    def willRespondWith(status: Int, headers: Map[String, String], body: Option[String]): ScalaPactInteraction = new ScalaPactInteraction(
      description,
      providerState,
      request,
      ScalaPactResponse(status, headers, body)
    )

    def finalise: ScalaPactInteractionFinal = ScalaPactInteractionFinal(description, providerState, request, response)
  }

  case class ScalaPactDescriptionFinal(consumer: String, provider: String, interactions: List[ScalaPactInteractionFinal], options: ScalaPactOptions)
  case class ScalaPactInteractionFinal(description: String, providerState: Option[String], request: ScalaPactRequest, response: ScalaPactResponse)

  object ScalaPactRequest {
    val default = ScalaPactRequest(GET, "/", None, Map.empty, None)
  }
  case class ScalaPactRequest(method: ScalaPactMethod, path: String, query: Option[String], headers: Map[String, String], body: Option[String])

  object ScalaPactResponse {
    val default = ScalaPactResponse(200, Map.empty, None)
  }
  case class ScalaPactResponse(status: Int, headers: Map[String, String], body: Option[String])

  object ScalaPactOptions {
    val DefaultOptions = ScalaPactOptions(writePactFiles = true)
  }
  case class ScalaPactOptions(writePactFiles: Boolean)

  sealed trait ScalaPactMethod {
    val method: String
  }
  case object GET extends ScalaPactMethod { val method = "GET" }
  case object PUT extends ScalaPactMethod { val method = "PUT" }
  case object POST extends ScalaPactMethod { val method = "POST" }
  case object DELETE extends ScalaPactMethod { val method = "DELETE" }

}