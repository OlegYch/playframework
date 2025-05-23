/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test

import org.apache.pekko.annotation.ApiMayChange
import play.api._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.routing.Router

/**
 * Creates an [[Application]]. Usually created by a helper in [[ApplicationFactories]].
 */
@ApiMayChange trait ApplicationFactory {

  /** Creates an [[Application]]. */
  def create(): Application
}

/**
 * Mixin with helpers for creating [[ApplicationFactory]] objects.
 */
@ApiMayChange trait ApplicationFactories {
  final def withGuiceApp(builder: GuiceApplicationBuilder): ApplicationFactory = new ApplicationFactory {
    override def create(): Application = builder.build()
  }

  final def withComponents(components: => BuiltInComponents): ApplicationFactory = new ApplicationFactory {
    override def create(): Application = components.application
  }

  final def withRouter(createRouter: BuiltInComponents => Router): ApplicationFactory =
    withConfigAndRouter(Map.empty)(createRouter)

  final def withConfigAndRouter(
      extraConfig: Map[String, Any]
  )(createRouter: BuiltInComponents => Router): ApplicationFactory = withComponents {
    val context = ApplicationLoader.Context.create(
      environment = Environment.simple(),
      initialSettings = extraConfig.asInstanceOf[Map[String, AnyRef]]
    )
    new BuiltInComponentsFromContext(context) with NoHttpFiltersComponents {
      override lazy val router: Router = createRouter(this)
    }
  }

  final def withAction(createAction: DefaultActionBuilder => Action[?]): ApplicationFactory = withRouter {
    (components: BuiltInComponents) =>
      val action = createAction(components.defaultActionBuilder)
      Router.from { case _ => action }
  }

  final def withResult(result: Result): ApplicationFactory = withAction { (Action: DefaultActionBuilder) =>
    Action { result }
  }
}

@ApiMayChange object ApplicationFactory extends ApplicationFactories
