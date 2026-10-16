package tv.own.owntv.ui.images

import coil3.annotation.ExperimentalCoilApi
import coil3.network.CacheStrategy
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.request.Options

/** Images only: transient HTTP errors must not become permanent missing channel logos.
 * Good entries retain Coil's cache behaviour; old failure entries get a fresh request. */
@OptIn(ExperimentalCoilApi::class)
internal object SuccessfulImageCacheStrategy : CacheStrategy {
    override suspend fun read(cacheResponse: NetworkResponse, networkRequest: NetworkRequest,
        options: Options): CacheStrategy.ReadResult =
        if (cacheResponse.code in 200..299) CacheStrategy.DEFAULT.read(cacheResponse, networkRequest, options)
        else CacheStrategy.ReadResult(networkRequest)

    override suspend fun write(cacheResponse: NetworkResponse?, networkRequest: NetworkRequest,
        networkResponse: NetworkResponse, options: Options): CacheStrategy.WriteResult {
        if (networkResponse.code in 200..299)
            return CacheStrategy.DEFAULT.write(cacheResponse, networkRequest, networkResponse, options)
        if (networkResponse.code == 304 && cacheResponse != null && cacheResponse.code in 200..299) {
            val revalidated = CacheStrategy.DEFAULT.write(cacheResponse, networkRequest, networkResponse, options)
            // 304 reuses a successful body: preserve its status when updating cached metadata.
            val response = revalidated.response ?: return CacheStrategy.WriteResult.DISABLED
            return CacheStrategy.WriteResult(response.copy(code = cacheResponse.code))
        }
        return CacheStrategy.WriteResult.DISABLED
    }
}
