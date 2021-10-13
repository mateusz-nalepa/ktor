/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.http.content

import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.util.pipeline.*

internal actual fun PipelineContext<Any, ApplicationCall>.platformTransformDefaultContent(
    value: Any
): OutgoingContent? = null
