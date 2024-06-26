package org.http4k.connect.github.api

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.base64Decoded
import org.http4k.base64Encode
import org.http4k.client.JavaHttpClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.github.GitHubToken
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.format.Moshi.auto
import java.io.File

private val gitHub = GitHub.Http2()

open class GitHubRepo(val domain: String, var branch: String = "master") {


    // https://docs.github.com/en/rest/repos/contents?apiVersion=2022-11-28#get-repository-content

//    fun getContent(path: String, branch: String = this@GitHubRepo.branch) = gitHub(GetContentTree(path, branch))
//
//    inner class GetContentTree(path: String, branch: String) : GitHubAction<ContentTree> {
//        val responseBody = Body.auto<ContentTree>().toLens()
//        val uri = Uri.of("$api/$domain/contents/$path?ref=$branch")
//
//        override fun toRequest() = Request(Method.GET, uri)
//
//        override fun toResult(response: Response) = when {
//            response.status.successful -> Success(responseBody(response))
//            else -> Failure(RemoteFailure(Method.GET, uri, response.status, response.bodyString()))
//        }
//    }

    // https://docs.github.com/en/rest/git/refs?apiVersion=2022-11-28#get-a-reference

    val gitRef: Result<GetGitRefData, RemoteFailure>
        get() = getGitRef()

    fun getGitRef(branch: String = this@GitHubRepo.branch) = gitHub(GetGitRef(branch))

    inner class GetGitRef(branch: String) : GitHubAction<GetGitRefData> {
        val responseBody = Body.auto<GetGitRefData>().toLens()
        val uri = Uri.of("$api/$domain/git/refs/heads/$branch")

        override fun toRequest() = Request(Method.GET, uri)

        override fun toResult(response: Response) = when {
            response.status.successful -> Success(responseBody(response))
            else -> Failure(RemoteFailure(Method.GET, uri, response.status, response.bodyString()))
        }
    }


    // https://docs.github.com/en/rest/git/refs?apiVersion=2022-11-28#create-a-reference

    fun createRef(ref: String, sha: String) = gitHub(CreateRef(ref, sha))

    inner class CreateRef(val ref: String, val sha: String) : GitHubAction<CreateRefData> {
        val responseBody = Body.auto<CreateRefData>().toLens()
        val uri = Uri.of("$api/$domain/git/refs")

        override fun toRequest() = Request(Method.POST, uri)
            .body("""{"ref": "refs/heads/$ref", "sha": "$sha"}""")

        override fun toResult(response: Response) = when {
            response.status.successful -> Success(responseBody(response))
            else -> Failure(RemoteFailure(Method.POST, uri, response.status, response.bodyString()))
        }
    }


    // https://docs.github.com/en/rest/repos/contents?apiVersion=2022-11-28#create-or-update-file-contents

    fun uploadContent(path: String, message: String,
                      content: File, sha: String? = null,
                      branch: String = this@GitHubRepo.branch,
                      committer: UploadContentData.Person? = null,
                      author: UploadContentData.Person? = committer) = gitHub(UploadContent(path, message, content, sha, branch, committer, author))

    inner class UploadContent(path: String, val message: String,
                              val content: File, val sha: String?,
                              val branch: String,
                              val committer: UploadContentData.Person?,
                              val author: UploadContentData.Person?) : GitHubAction<UploadContentData> {
        val responseBody = Body.auto<UploadContentData>().toLens()
        val uri = Uri.of("$api/$domain/contents/$path")
        val body = buildString {
            append("""{"message":"$message","content":"${content.readBytes().base64Encode()}",""")
            sha?.let { append(""""sha":"$it",""") }
            append(""""branch":"$branch"""")
            committer?.let { append(""","committer":${it.body}""") }
            author?.let { append(""","author":${it.body}""") }
            append("""}""")
        }

        override fun toRequest() = Request(Method.PUT, uri).body(body)

        override fun toResult(response: Response) = when {
            response.status.successful -> Success(responseBody(response))
            else -> Failure(RemoteFailure(Method.PUT, uri, response.status, response.bodyString()))
        }
    }


    // https://docs.github.com/en/rest/pulls/pulls?apiVersion=2022-11-28#create-a-pull-request

    fun createPR(title: String? = null, head: String, headRepo: String? = null,
                 base: String, body: String? = null, maintainerCanModify: Boolean? = null,
                 draft: Boolean? = null, issue: Int? = null) = gitHub(CreatePR(title, head, headRepo, base, body, maintainerCanModify, draft, issue))

    inner class CreatePR(title: String?, head: String, headRepo: String?,
                         base: String, body: String?, maintainerCanModify: Boolean?,
                         draft: Boolean?, issue: Int?) : GitHubAction<PushRequest> {
        val responseBody = Body.auto<PushRequest>().toLens()
        val uri = Uri.of("$api/$domain/pulls")
        val body = buildString {
            append("{")
            title?.let { append(""""title":"$it",""") }
            append(""""head":"$head",""")
            headRepo?.let { append(""""head_repo":"$it",""") }
            append(""""base":"$base"""")
            body?.let { append(""","head_repo":"$it"""") }
            maintainerCanModify?.let { append(""","maintainer_can_modify":"$it"""") }
            draft?.let { append(""","draft":"$it",""") }
            issue?.let { append(""","issue":"$it"""") }
            append("""}""")
        }

        override fun toRequest() = Request(Method.POST, uri).body(body)

        override fun toResult(response: Response) = when {
            response.status.successful -> Success(responseBody(response))
            else -> Failure(RemoteFailure(Method.POST, uri, response.status, response.bodyString()))
        }
    }


    // https://docs.github.com/en/rest/pulls/pulls?apiVersion=2022-11-28#create-a-pull-request

    val pullRequests: Result<PullRequests, RemoteFailure>
        get() = gitHub(GetPRs())

    inner class GetPRs : GitHubAction<PullRequests> {
        val responseBody = Body.auto<PullRequests>().toLens()
        val uri = Uri.of("$api/$domain/pulls")

        override fun toRequest() = Request(Method.GET, uri)

        override fun toResult(response: Response) = when {
            response.status.successful -> Success(responseBody(response))
            else -> Failure(RemoteFailure(Method.GET, uri, response.status, response.bodyString()))
        }
    }


    // https://docs.github.com/en/rest/commits/commits?apiVersion=2022-11-28#get-a-commit

    infix fun getCommit(ref: String) = gitHub(GetCommit(ref))

    inner class GetCommit(ref: String) : GitHubAction<CommitData> {
        val responseBody = Body.auto<CommitData>().toLens()
        val uri = Uri.of("$api/$domain/commits/$ref")

        override fun toRequest() = Request(Method.GET, uri)

        override fun toResult(response: Response) = when {
            response.status.successful -> Success(responseBody(response))
            else -> Failure(RemoteFailure(Method.GET, uri, response.status, response.bodyString()))
        }
    }


    // https://docs.github.com/en/rest/pulls/pulls?apiVersion=2022-11-28#merge-a-pull-request

    fun mergePullRequest(number: Int,
                         commitTitle: String? = null, commitMessage: String? = null,
                         sha: String? = null, mergeMethod: MergeMethod? = null) = gitHub(MergePullRequest(number, commitTitle, commitMessage, sha, mergeMethod))

    inner class MergePullRequest(number: Int,
                                 commitTitle: String? = null, commitMessage: String? = null,
                                 sha: String? = null, mergeMethod: MergeMethod? = null) : GitHubAction<PullRequestMergeResult> {
        val responseBody = Body.auto<PullRequestMergeResult>().toLens()
        val uri = Uri.of("$api/$domain/pulls/$number/merge")
        val body = when {
            commitTitle == null && commitMessage == null && sha == null && mergeMethod == null -> ""
            else -> buildString {
                append("{")
                commitTitle?.let { append(""""commit_title":"$it",""") }
                commitMessage?.let { append(""""commit_message":"$it",""") }
                sha?.let { append(""""sha":"$it",""") }
                mergeMethod?.let { append(""""merge_method":"$it"""") }
                append("""}""")
            }
        }

        override fun toRequest() = Request(Method.PUT, uri)

        override fun toResult(response: Response) = when {
            response.status.successful -> Success(responseBody(response))
            else -> Failure(RemoteFailure(Method.PUT, uri, response.status, response.bodyString()))
        }
    }


    // https://docs.github.com/en/rest/git/refs?apiVersion=2022-11-28#delete-a-reference

    infix fun deleteRef(ref: String) = gitHub(DeleteRef(ref))
    infix fun deleteBranch(branch: String) = gitHub(DeleteRef("heads/$branch"))

    inner class DeleteRef(ref: String) : GitHubAction<Status> {
        val uri = Uri.of("$api/$domain/git/refs/$ref")

        override fun toRequest() = Request(Method.DELETE, uri)

        override fun toResult(response: Response) = when {
            response.status.successful -> Success(response.status)
            else -> Failure(RemoteFailure(Method.DELETE, uri, response.status, response.bodyString()))
        }
    }

    companion object {
        val api = "https://api.github.com/repos"
    }
}

fun GitHub.Companion.Http2(token: GitHubToken = GitHubToken.of(System.getenv("TOKEN")),
                           http: HttpHandler = JavaHttpClient(),
                           authScheme: String = "token") =
    object : GitHub {
        //        private val routedHttp = ClientFilters.SetBaseUriFrom(Uri.of("https://api.github.com"))
        //            .then(http)

        override fun <R> invoke(action: GitHubAction<R>) =
            action.toResult(
                http(action.toRequest()
                         .header("Accept", "application/vnd.github+json")
                         .header("Authorization", "$authScheme $token")))
    }
