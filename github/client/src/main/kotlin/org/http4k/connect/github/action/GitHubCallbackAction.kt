package org.http4k.connect.github.action

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure

@Http4kConnectAction
interface GitHubCallbackAction<T> : Action<Result<T, RemoteFailure>>