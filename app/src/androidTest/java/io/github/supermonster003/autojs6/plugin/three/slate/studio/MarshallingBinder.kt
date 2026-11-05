package io.github.supermonster003.autojs6.plugin.three.slate.studio

import android.os.IBinder
import android.os.IInterface

/**
 * An [IBinder] that hides the local object of a service in the test process, so `Stub.asInterface` returns the
 * generated proxy and every call is written to and read from Parcels as from a host (roadmap P0.3).
 * zh-CN: 隐藏测试进程中服务本地对象的 [IBinder], 让 `Stub.asInterface` 返回生成的代理, 每次调用都像来自宿主那样经 Parcel
 * 写入与读出 (路线图 P0.3).
 */
class MarshallingBinder(private val target: IBinder) : IBinder by target {

    override fun queryLocalInterface(descriptor: String): IInterface? = null
}
